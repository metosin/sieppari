(ns sieppari.chain-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as s]
            [sieppari.chain :as sc]))

;;
;; Following tests use a test-chain that has some interceptors
;; that fail on each stage function (enter, leave, error). The
;; idea is that the tests override the expected stage functions
;; with test specific function. This ensures that no unexpected
;; stage functions are called.
;;

; Make an interceptor with given name and set all stage functions
; to report unexpected invocation. Tests should override expected
; stages.

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

(defn always-throw [ctx]
  (throw (ex-info "oh no" {::error-marker true})))

;; Helper: return error handler function that ensures
;; that `ctx` contains an exception caused by `always-throw`,
;; clears the exception and sets response to given response:

(defn handle-error [response]
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
  (fact "enable all enter and leave stages, use `inc` as handler"
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] identity)
        (assoc-in [h-index] inc)
        (assoc-in [c-index :leave] identity)
        (assoc-in [b-index :leave] identity)
        (assoc-in [a-index :leave] identity)
        (sc/execute 41))
    => 42))

(deftest enter-b-causes-exception-test
  (fact ":b causes an exception"
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] always-throw)
        (assoc-in [b-index :error] identity)
        (assoc-in [a-index :error] identity)
        (sc/execute 41))
    => (throws-ex-info "oh no")))

(deftest enter-c-causes-exception-a-handles-test
  (fact ":c enter causes an exception, :b sees error, :a handles"
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] identity)
        (assoc-in [a-index :error] (handle-error :fixed-by-a))
        (sc/execute 41))
    => :fixed-by-a))

(deftest enter-c-causes-exception-b-handles-test
  (fact ":c enter causes an exception, :b handles"
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] (handle-error :fixed-by-b))
        (assoc-in [a-index :leave] identity)
        (sc/execute 41))
    => :fixed-by-b))

(deftest handler-causes-exception-b-handles-test
  (fact
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] identity)
        (assoc-in [c-index :enter] identity)
        (assoc-in [h-index] always-throw)
        (assoc-in [c-index :error] identity)
        (assoc-in [b-index :error] (handle-error :fixed-by-b))
        (assoc-in [a-index :leave] identity)
        (sc/execute 41))
    => :fixed-by-b))

(deftest enter-b-sets-response-test
  (fact ":b sets the response, no invocation of :c nor :handler"
    (-> test-chain
        (assoc-in [a-index :enter] identity)
        (assoc-in [b-index :enter] (fn [ctx] (s/terminate ctx :response-by-b)))
        (assoc-in [b-index :leave] identity)
        (assoc-in [a-index :leave] identity)
        (sc/execute 41))
    => :response-by-b))

(defn make-logging-interceptor [name]
  {:name name
   :enter (fn [ctx]
            (update ctx :request conj [:enter name]))
   :leave (fn [ctx]
            (update ctx :response conj [:leave name]))})

(defn logging-handler [request]
  (conj request [:handler]))

(deftest inject-interceptor-test
  (fact ":b injects interceptor :x to chain, ensure the order is correct"
    (-> [(make-logging-interceptor :a)
         {:enter (fn [ctx] (s/inject ctx (make-logging-interceptor :x)))}
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :x]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :x]
        [:leave :a]]))

; TODO: figure out how enqueue should work? Should enqueue add interceptors just
; before the handler?
#_
(deftest enqueue-interceptor-test
  (fact ":b enqueues interceptor :x to chain, ensure the order is correct"
    (-> [(make-logging-interceptor :a)
         {:enter (fn [ctx] (s/enqueue ctx (make-logging-interceptor :x)))}
         (make-logging-interceptor :c)
         logging-handler]
        (s/into-interceptors)
        (sc/execute []))
    => [[:enter :a]
        [:enter :c]
        [:enter :x]
        [:handler]
        [:leave :x]
        [:leave :c]
        [:leave :a]]))

