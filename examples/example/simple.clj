(ns example.simple
  (:require [sieppari.core :as sc]
            [sieppari.execute.sync :as ses]))

;;
;; Demonstrate functionality, mainly for documentation purposes:
;;

;; Simple interceptor, in enter update value in `[:request :in]` with `inc`:

(def inc-in-interceptor
  {:enter (fn [ctx]
            (update-in ctx [:request :in] inc))})

;; Simple handler, take `:in` from request, apply `inc`, and
;; return an map with `:out`.

(defn handler [request]
  {:out (inc (:in request))})

(def interceptor-chain (sc/into-interceptors [inc-in-interceptor
                                              handler]))

(ses/execute interceptor-chain {:in 40})
;=> {:out 42}

;;
;; Simple example:
;;

(defn make-interceptor [name]
  {:enter (fn [ctx] (println "ENTER:" name) ctx)
   :leave (fn [ctx] (println "LEAVE:" name) ctx)
   :error (fn [ctx] (println "ERROR:" name) ctx)})

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "handler response")]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  LEAVE: :c
;  LEAVE: :b
;  LEAVE: :a
;=> "handler response"

;;
;; Handler causes an exception:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  ERROR: :c
;  ERROR: :b
;  ERROR: :a
;=> CompilerException clojure.lang.ExceptionInfo: oh no {}

;;
;; Handler :b handles the exception:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :error (fn [ctx]
                         (println "ERROR: :b - this handles the exception")
                         (-> ctx
                             (dissoc :error)
                             (assoc :response :fixed-by-b)))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  ERROR: :c
;  ERROR: :b - this handles the exception
;  LEAVE: :a
;=> :fixed-by-b

;;
;; Interceptor can terminate execution in `enter` phase:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :enter (fn [ctx]
                         (println "ENTER: :b - short circuit")
                         (assoc ctx :response :short-circuit-by-b))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "response from handler")]))

(ses/execute interceptor-chain "request message")
; Prints
;   ENTER: :a
;   ENTER: :b - short circuit
;   LEAVE: :a
;=> :short-circuit-by-b


;;
;; Interceptors can (and usually do) modify the `ctx`:
;;

(defn make-interceptor [name]
  {:enter (fn [ctx] (update ctx :stack conj [:enter name]))
   :leave (fn [ctx] (update ctx :stack conj [:leave name]))
   :error (fn [ctx] (update ctx :stack conj [:error name]))})

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "ENTER: request =" (pr-str request))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints:
;  ENTER: request = {}
;=> {:response :response-from-handler}

;;
;; Publish something from interceptor to handler:
;;

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :stack-so-far] (:stack ctx)))}
     (fn [request]
       (println "ENTER: stack =" (pr-str (:stack-so-far request)))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints: ENTER: stack = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler}

;;
;; Publish something to response:
;;

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))
      :leave (fn [ctx]
               (assoc-in ctx [:response :final-stack] (:stack ctx)))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :stack-so-far] (:stack ctx)))}
     (fn [request]
       (println "ENTER: stack =" (pr-str (:stack-so-far request)))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints: ENTER: stack = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler
;    :final-stack [[:enter :a]
;                  [:enter :b]
;                  [:enter :c]
;                  [:leave :c]
;                  [:leave :b]
;                  [:leave :a]]}

