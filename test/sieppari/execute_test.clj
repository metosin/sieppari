(ns sieppari.execute-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as c]
            [sieppari.execute :as e]))

(def try-f #'e/try-f)
(def completed? #'e/completed?)
(def error? #'e/error?)
(def enter #'e/enter)
(def leave #'e/leave)

(deftest try-f-test
  (fact "f succeeds"
    (try-f identity {})
    => (just {}))

  (let [e (RuntimeException. "oh no")
        f (fn [_] (throw e))]

    (fact "f fails with exception"
      (try-f f {}) => (just {:exception e}))

    (fact "same, but use error? predicate"
      (try-f f {}) => error?)))

(deftest enter-test
  )

(deftest leave-test
  )

(deftest execute-test
  )

