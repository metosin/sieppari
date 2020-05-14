(ns sieppari.interceptor-test
  (:require [clojure.test :refer [deftest is testing]]
            [sieppari.interceptor :as si])
  #?(:clj (:import (sieppari.interceptor Interceptor))))

(defn make-interceptor [value]
  {:enter (fn [ctx]
            (update ctx :foo + value))})

(defn make-handler [value]
  (fn [request]
    (+ request value)))

(deftest into-interceptor-test
  (is (instance? #?(:clj Interceptor :cljs si/Interceptor) (si/into-interceptor {})) "result is  record")

  (is (fn? (:enter (si/into-interceptor str))) "functions can be made to interceptors")

  (is (= {:request 41 :response 42}
         (-> inc si/into-interceptor :enter (apply [{:request 41}]))) "functions are treated as request handlers")

  (is (= {:foo 42}
         (-> [make-interceptor 10]
             si/into-interceptor
             :enter
             (apply [{:foo 32}]))) "vectors are evaluated, interceptor factory case")

  (is (= {:request 32 :response 42}
         (-> [make-handler 10]
             si/into-interceptor
             :enter
             (apply [{:request 32}]))) "vectors are evaluated, handler factory case")

  (let [i (si/into-interceptor {:name "test"})]
    (is (identical? i (si/into-interceptor i)) "interceptors are already interceptors"))

  (is (nil? (si/into-interceptor nil)) "nil punning"))
