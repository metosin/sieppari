(ns sieppari.core.async-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.context :as s]
            [sieppari.core :as sc]
            [sieppari.async :as sa]
            [clojure.core.async :as a :refer [go <! <!!]]))

;;
;; TODO: Move async stuff to its own module.
;;

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (go (f (<! c)))))

(defn make-logging-interceptor [name]
  {:name name
   :enter (fn [ctx] (update ctx :request conj [:enter name]))
   :leave (fn [ctx] (update ctx :response conj [:leave name]))
   :error (fn [ctx] (update ctx :response conj [:error name]))})

(defn make-async-logging-interceptor [name]
  {:name name
   :enter (fn [ctx] (go (update ctx :request conj [:enter name])))
   :leave (fn [ctx] (go (update ctx :response conj [:leave name])))
   :error (fn [ctx] (go (update ctx :response conj [:error name])))})

(defn logging-handler [request]
  (conj request [:handler]))

(defn async-logging-handler [request]
  (go (conj request [:async-handler])))

(deftest setup-test-test
  (fact
    (-> [(make-logging-interceptor :a)
         (make-logging-interceptor :b)
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :b]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :b]
        [:leave :a]]))

(deftest async-enter-sync-execute-test
  (fact
    (-> [(make-logging-interceptor :a)
         (assoc (make-logging-interceptor :b)
           :enter (fn [ctx]
                    (go
                      (update ctx :request conj [:enter :async-b]))))
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :async-b]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :b]
        [:leave :a]]))

(deftest async-leave-sync-execute-test
  (fact
    (-> [(make-logging-interceptor :a)
         (assoc (make-logging-interceptor :b)
           :leave (fn [ctx]
                    (go
                      (update ctx :response conj [:leave :async-b]))))
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :b]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :async-b]
        [:leave :a]]))

(deftest async-b-sync-execute-test
  (fact
    (-> [(make-logging-interceptor :a)
         (make-async-logging-interceptor :async-b)
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :async-b]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :async-b]
        [:leave :a]]))

(deftest async-all-the-way-execute-test
  (fact
    (-> [(make-async-logging-interceptor :async-a)
         (make-async-logging-interceptor :async-b)
         (make-async-logging-interceptor :async-c)
         async-logging-handler]
        (sc/execute []))
    => [[:enter :async-a]
        [:enter :async-b]
        [:enter :async-c]
        [:async-handler]
        [:leave :async-c]
        [:leave :async-b]
        [:leave :async-a]]))

(deftest async-all-the-way-async-execute-test
  (let [p (promise)]
    (-> [(make-async-logging-interceptor :async-a)
         (make-async-logging-interceptor :async-b)
         (make-async-logging-interceptor :async-c)
         async-logging-handler]
        (sc/execute [] (partial deliver p)))
    (fact {:timeout 100}
      (deref p) => [[:enter :async-a]
                    [:enter :async-b]
                    [:enter :async-c]
                    [:async-handler]
                    [:leave :async-c]
                    [:leave :async-b]
                    [:leave :async-a]])))
