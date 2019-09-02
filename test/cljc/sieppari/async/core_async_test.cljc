(ns sieppari.async.core-async-test
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is #?(:cljs async)]]
            [sieppari.async :as as]
            [sieppari.async.core-async]
            [clojure.core.async :as a]))

(deftest async?-test
  (is (as/async? (a/go "foo"))))

#?(:clj
   (deftest core-async-continue-clj-promise-test
     (let [respond (promise)]
       (as/continue (a/go "foo") (partial deliver respond))
       (is (= "foo" @respond))))
   :cljs
   (deftest core-async-continue-cljs-callback-test
     (async done
       (is (as/continue (a/go "foo")
                        (fn [response]
                          (is (= "foo" response))
                          (done)))))))

#?(:clj
   (deftest await-test
     (is (= "foo" (as/await (a/go "foo"))))))
