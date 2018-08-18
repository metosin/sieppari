(ns sieppari.async.core-async-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.async :as as]
            [sieppari.async.core-async]
            [clojure.core.async :as a]))

(deftest async?-test
  (fact
    (as/async? (a/go "foo")) => true))

(deftest continue-test
  (let [p (promise)]
    (as/continue (a/go "foo")
                 (partial deliver p))
    (fact {:timeout 100}
      @p => "foo")))

(deftest await-test
  (fact
    (as/await (a/go "foo")) => "foo"))
