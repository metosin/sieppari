(ns example.simple
  (:require [sieppari.core :as s]
            [sieppari.execute :as se]))

;;
;; Demonstrate functionality, mainly for documentation purposes:
;;

;; Simple interceptor, in enter update value in `[:request :x]` with `inc`:

(def inc-x-interceptor
  {:enter (fn [ctx]
            (update-in ctx [:request :x] inc))})

;; Simple handler, take `:x` from request, apply `inc`, and
;; return an map with `:y`.

(defn handler [request]
  {:y (inc (:x request))})

(def interceptor-chain (s/into-interceptors [inc-x-interceptor
                                             handler]))

(se/execute interceptor-chain {:x 40})
;=> {:y 42}

;;
;; Simple example:
;;

(defn make-interceptor [name]
  {:enter (fn [ctx] (println "ENTER:" name) ctx)
   :leave (fn [ctx] (println "LEAVE:" name) ctx)
   :error (fn [ctx] (println "ERROR:" name) ctx)})

(def interceptor-chain
  (s/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "handler response")]))

(se/execute interceptor-chain "request message")
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
  (s/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(comment
  (se/execute interceptor-chain "request message"))
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
  (s/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :error (fn [ctx]
                         (println "ERROR: :b - this handles the exception")
                         (assoc ctx :response :fixed-by-b
                                    :error nil))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(se/execute interceptor-chain "request message")
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
  (s/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :enter (fn [ctx]
                         (println "ENTER: :b - short circuit")
                         (assoc ctx :response :short-circuit-by-b))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "response from handler")]))

(se/execute interceptor-chain "request message")
; Prints
;   ENTER: :a
;   ENTER: :b - short circuit
;   LEAVE: :a
;=> :short-circuit-by-b


;;
;; Interceptors can (and usually do) modify the `ctx`:
;;

(defn make-interceptor [name]
  {:enter (fn [ctx] (update ctx :log conj [:enter name]))
   :leave (fn [ctx] (update ctx :log conj [:leave name]))
   :error (fn [ctx] (update ctx :log conj [:error name]))})

(def interceptor-chain
  (s/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :log []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "ENTER: request =" (pr-str request))
       {:response :response-from-handler})]))

(se/execute interceptor-chain {})
; Prints:
;  ENTER: request = {}
;=> {:response :response-from-handler}

;;
;; Publish something from interceptor to handler:
;;

(def interceptor-chain
  (s/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :log []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :log-so-far] (:log ctx)))}
     (fn [request]
       (println "ENTER: log =" (pr-str (:log-so-far request)))
       {:response :response-from-handler})]))

(se/execute interceptor-chain {})
; Prints: ENTER: log = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler}

;;
;; Publish something to response:
;;

(def interceptor-chain
  (s/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :log []))
      :leave (fn [ctx]
               (assoc-in ctx [:response :final-log] (:log ctx)))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :log-so-far] (:log ctx)))}
     (fn [request]
       (println "ENTER: log =" (pr-str (:log-so-far request)))
       {:response :response-from-handler})]))

(se/execute interceptor-chain {})
; Prints: ENTER: log = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler
;    :final-log [[:enter :a]
;                [:enter :b]
;                [:enter :c]
;                [:leave :c]
;                [:leave :b]
;                [:leave :a]]}
