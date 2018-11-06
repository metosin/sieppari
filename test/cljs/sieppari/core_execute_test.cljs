(ns sieppari.core-execute-test
  (:require [clojure.test :refer-macros [deftest is testing async]]
            [sieppari.core :as sc]
            [sieppari.context :as sctx]))

;; Following tests use a test-chain that has some interceptors
;; that fail on each stage function (enter, leave, error). The
;; idea is that the tests override the expected stage functions
;; with test specific function. This ensures that no unexpected
;; stage functions are called.
;;

;; Make an interceptor with given name and set all stage functions
;; to report unexpected invocation. Tests should override expected
;; stages.

(defn unexpected [name stage]
  (fn [ctx]
    (throw (ex-info "unexpected invocation"
                    {:name name
                     :stage stage
                     :ctx ctx}))))

(defn make-test-interceptor [name]
  {:name name
   :enter (unexpected name :enter)
   :leave (unexpected name :leave)
   :error (unexpected name :error)})

; Test stack with three interceptors and a handler:

(def test-chain [(make-test-interceptor :a)
                 (make-test-interceptor :b)
                 (make-test-interceptor :c)
                 (unexpected :handler nil)])

(def a-index 0)
(def b-index 1)
(def c-index 2)
(def h-index 3)

;; Helper: always throws an exception with specific marker
;; in data part:

(defn fail! [& _]
  (throw (ex-info "Should never be called" {})))

(def error (ex-info "oh no" {::error-marker true}))

(defn always-throw [ctx]
  (throw error))

;; Helper: return error handler function that ensures
;; that `ctx` contains an exception caused by `always-throw`,
;; clears the exception and sets response to given response:

(defn error-handler-interceptor [response]
  (fn [ctx]
    (assert (not (some? (:response ctx))))
    (assert (-> ctx :error ex-data (= {::error-marker true})))
    (-> ctx
        (dissoc :error)
        (assoc :response response))))

;;
;; Tests:
;;

(deftest happy-case-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] identity)
        (assoc-in [h-index] inc)
        (assoc-in [c-index :leave] identity)
        (assoc-in [b-index :leave] identity)
        (assoc-in [a-index :leave] identity)
        (sc/execute 41
                    (fn [response]
                      (is (= 42 response) "enable all enter and leave stages, use `inc` as handler")
                      (done))
                    fail!))))

(deftest enter-b-causes-exception-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] always-throw)
        (assoc-in [b-index :error] identity)
        (assoc-in [a-index :error] identity)
        (sc/execute 41
                    fail!
                    (fn [err]
                      (is (= error err) ":b causes an exception")
                      (done))))))

(deftest enter-c-causes-exception-a-handles-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] identity)
        (assoc-in [a-index :error] (error-handler-interceptor :fixed-by-a))
        (sc/execute 41
                    (fn [response]
                      (is (= :fixed-by-a response) ":c enter causes an exception, :b sees error, :a handles")
                      (done))
                    fail!))))

(deftest enter-c-causes-exception-b-handles-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] (error-handler-interceptor :fixed-by-b))
        (assoc-in [a-index :leave] identity)
        (sc/execute 41
                    (fn [response]
                      (is (= :fixed-by-b response) ":c enter causes an exception, :b handles")
                      (done))
                    fail!))))

(deftest handler-causes-exception-b-handles-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] identity)
        (assoc-in [h-index] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] (error-handler-interceptor :fixed-by-b))
        (assoc-in [a-index :leave] identity)
        (sc/execute 41
                    (fn [response]
                      (is (= :fixed-by-b response) ":c enter causes an exception, :b handles")
                      (done))
                    fail!))))

(deftest enter-b-sets-response-test
  (async done
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] (fn [ctx] (sctx/terminate ctx :response-by-b)))
        (assoc-in [b-index :leave] identity)
        (assoc-in [a-index :leave] identity)
        (sc/execute 41
                    (fn [response]
                      (is (= :response-by-b response) ":b sets the response, no invocation of :c nor :handler")
                      (done))
                    fail!))))

(deftest nil-response-test
  (async done
    (sc/execute [(constantly nil)] {}
                (fn [response]
                  (is (= nil response) "nil response is allowed")
                  (done))
                fail!)))

(deftest nil-interceptors-test
  (async done
    (sc/execute nil {}
                (fn [response]
                  (is (= nil response) "interceptor chain can be nil")
                  (done))
                fail!)))

(deftest empty-interceptors-test
  (async done
    (sc/execute [] {}
                (fn [response]
                  (is (= nil response) "interceptor chain can be empty")
                  (done))
                fail!)))

(defn make-logging-interceptor [name]
  {:name name
   :enter (fn [ctx]
            (update ctx :request conj [:enter name]))
   :leave (fn [ctx]
            (update ctx :response conj [:leave name]))})

(defn logging-handler [request]
  (conj request [:handler]))

(deftest inject-interceptor-test
  (async done
    (-> [(make-logging-interceptor :a)
         {:enter (fn [ctx] (sctx/inject ctx (make-logging-interceptor :x)))}
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []
                    (fn [response]
                      (is (= [[:enter :a]
                              [:enter :x]
                              [:enter :c]
                              [:handler]
                              [:leave :c]
                              [:leave :x]
                              [:leave :a]]
                             response) ":b injects interceptor :x to chain, ensure the order is correct")
                      (done))
                    fail!))))

;; TODO: figure out how enqueue should work? Should enqueue add interceptors just
;; before the handler?

#_
(deftest enqueue-interceptor-test
  (fact ":b enqueues interceptor :x to chain, ensure the order is correct"
    (-> [(make-logging-interceptor :a)
         {:enter (fn [ctx] (sc/enqueue ctx (make-logging-interceptor :x)))}
         (make-logging-interceptor :c)
         logging-handler]
        (sctx/into-interceptors)
        (sc/execute []))
    => [[:enter :a]
        [:enter :c]
        [:enter :x]
        [:handler]
        [:leave :x]
        [:leave :c]
        [:leave :a]]))
