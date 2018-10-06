(ns sieppari.async.promesa-test
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is #?(:cljs async)]]
            [sieppari.async :as as]
            [sieppari.async.promesa]
            [promesa.core :as p]))

(deftest async?-test
  (is (as/async? (p/promise 1))))

#?(:clj
   (deftest core-async-continue-cljs-callback-test
     (let [respond (promise)
           p (p/promise
              (fn [resolve _]
                (p/schedule 10 #(resolve "foo"))))]
       (as/continue p respond)
       (is (= @respond "foo"))))
   :cljs
   (deftest core-async-continue-cljs-callback-test
     (let [p (p/promise
              (fn [resolve _]
                (p/schedule 10 #(resolve "foo"))))]
       (async done
         (is (as/continue p (fn [response]
                              (is (= "foo" response))
                              (done))))))))

#?(:clj
   (deftest await-test
     (is (= "foo"
            (as/await (p/promise
                       (fn [resolve _]
                         (p/schedule 10 #(resolve "foo")))))))))
