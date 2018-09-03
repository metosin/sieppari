(ns sieppari.async.promesa-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.async :as as]
            [sieppari.async.promesa]
            [promesa.core :as p]))

(deftest async?-test
  (fact
    (as/async? (p/promise 1)) => true))

(deftest continue-test
  (let [respond (promise)
        p (p/promise
            (fn [resolve _]
              (p/schedule 10 #(resolve "foo"))))]
    (as/continue p respond)
    (fact {:timeout 100}
      @respond => "foo")))

(deftest await-test
  (fact
    (as/await (p/promise
                (fn [resolve _]
                  (p/schedule 10 #(resolve "foo")))))
    => "foo"))
