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

(deftest into-interceptor-test
  (fact "result is  record"
    (into-interceptor {}) => Interceptor)

  (fact "functions can be made to interceptors"
    (into-interceptor str) => {:enter fn?})

  (fact "functions are treated as request handlers"
    (-> inc into-interceptor :enter (apply [{:request 41}])) => {:response 42})

  (fact "vectors are evaluated, interceptor factory case"
    (-> [make-interceptor 10] into-interceptor :enter (apply [{:foo 32}])) => {:foo 42})

  (fact "vectors are evaluated, handler factory case"
    (-> [make-handler 10] into-interceptor :enter (apply [{:request 32}])) => {:response 42})

  (let [i (into-interceptor {})]
    (fact "interceptors are already interceptors"
      (into-interceptor i) => (partial identical? i)))

  (fact "nil punning"
    (into-interceptor nil) => nil))
