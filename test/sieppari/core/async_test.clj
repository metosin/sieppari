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

(defn logging-handler [request]
  (conj request [:handler]))

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

(deftest async-enter-test
  (fact ":b enter is async, call sync execute"
    (-> [(make-logging-interceptor :a)
         (assoc (make-logging-interceptor :b) :enter (fn [ctx] (go (update ctx :request conj [:enter :async-b]))))
         (make-logging-interceptor :c)
         logging-handler]
        (sc/execute []))
    => [[:enter :a]
        [:enter :async-b]
        [:enter :c]
        [:handler]
        [:leave :c]
        [:leave :b]
        [:leave :a]])
  (testing ":b enter is async, call async execute"
    (let [result (atom nil)]
      (-> [(make-logging-interceptor :a)
           (assoc (make-logging-interceptor :b) :enter (fn [ctx] (go (update ctx :request conj [:enter :async-b]))))
           (make-logging-interceptor :c)
           logging-handler]
          (sc/execute [] (partial reset! result)))
      (fact
        @result =eventually=> [[:enter :a]
                               [:enter :async-b]
                               [:enter :c]
                               [:handler]
                               [:leave :c]
                               [:leave :b]
                               [:leave :a]]))))

(deftest async-handler-test
  (fact "async handler , call sync execute"
    (-> [(make-logging-interceptor :a)
         (make-logging-interceptor :b)
         (make-logging-interceptor :c)
         (fn [request] (go (conj request [:async-handler])))]
        (sc/execute []))
    => [[:enter :a]
        [:enter :b]
        [:enter :c]
        [:async-handler]
        [:leave :c]
        [:leave :b]
        [:leave :a]])
  (testing "async handler, call async execute"
    (let [result (atom nil)]
      (-> [(make-logging-interceptor :a)
           (make-logging-interceptor :b)
           (make-logging-interceptor :c)
           (fn [request] (go (conj request [:async-handler])))]
          (sc/execute [] (partial reset! result)))
      (fact
        @result =eventually=> [[:enter :a]
                               [:enter :b]
                               [:enter :c]
                               [:async-handler]
                               [:leave :c]
                               [:leave :b]
                               [:leave :a]]))))
