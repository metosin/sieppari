(ns sieppari.execute.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.execute.core :refer :all]))

(deftest try-f-test
  (fact "f succeeds"
    (try-f identity {})
    => (just {}))

  (let [e (RuntimeException. "oh no")
        f (fn [_] (throw e))]
    (fact "f fails with exception"
      (try-f f {}) => (just {:exception e}))))

(deftest throw-if-error!-test
  (fact "if no exception in ctx, return just ctx"
    (throw-if-error! {})
    => (just {}))
  (fact "if there is an exception in ctx, throw it"
    (throw-if-error! {:exception (ex-info "oh no" {})})
    => (throws-ex-info "oh no")))
