(ns sieppari.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :refer :all])
  (:import (sieppari.core Interceptor)))

(defn make-interceptor [value]
  {:enter (fn [ctx]
            (update ctx :foo + value))})

(defn make-handler [value]
  (fn [request]
    (+ request value)))

(deftest -interceptor-test
  (fact "result is  record"
    (-interceptor {}) => Interceptor)

  (fact "functions can be made to interceptors"
    (-interceptor str) => {:enter fn?})

  (fact "functions are treated as request handlers"
    (-> inc -interceptor :enter (apply [{:request 41}])) => {:response 42})

  (fact "vectors are evaluated, interceptor factory case"
    (-> [make-interceptor 10] -interceptor :enter (apply [{:foo 32}])) => {:foo 42})

  (fact "vectors are evaluated, handler factory case"
    (-> [make-handler 10] -interceptor :enter (apply [{:request 32}])) => {:response 42})

  (let [i (-interceptor {})]
    (fact "interceptors are already interceptors"
      (-interceptor i) => (partial identical? i)))

  (fact "nil punning"
    (-interceptor nil) => nil))

(deftest terminate-test
  (let [queue (->> (into-interceptors [{:name :a} {:name :b}])
                   (into clojure.lang.PersistentQueue/EMPTY))]
    (fact
      (terminate {:queue queue})
      => (just {:queue clojure.lang.PersistentQueue/EMPTY}))
    (fact
      (terminate {:queue queue} :the-response)
      => (just {:queue clojure.lang.PersistentQueue/EMPTY
                :response :the-response}))))

(deftest inject-test
  (let [queue (->> (into-interceptors [{:name :a} {:name :b}])
                   (into clojure.lang.PersistentQueue/EMPTY))]
    (fact
      (inject {:queue queue} {:name :x})
      => {:queue [{:name :x}
                  {:name :a}
                  {:name :b}]})
    (fact
      (inject {:queue queue} {:name :x})
      => {:queue clojure.lang.PersistentQueue})))

; TODO: figure out how enqueue should work? Should enqueue add interceptors just
#_
(deftest enqueue-test
  (let [queue (into clojure.lang.PersistentQueue/EMPTY (into-interceptors [{:name :a} {:name :b}]))]
    (fact
      (enqueue {:queue queue} {:name :x})
      => {:queue [{:name :a}
                  {:name :b}
                  {:name :x}]})
    (fact
      (enqueue {:queue queue} {:name :x})
      => {:queue clojure.lang.PersistentQueue})))
