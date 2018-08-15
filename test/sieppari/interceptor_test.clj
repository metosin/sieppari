(ns sieppari.interceptor-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.interceptor :refer :all])
  (:import (sieppari.interceptor Interceptor)))

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
