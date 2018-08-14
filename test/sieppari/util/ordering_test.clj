(ns sieppari.util.ordering-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.util.ordering :refer [dependency-order]]))

(deftest dependency-order-test
  (fact "without any depends, all interceptors are included but the order in undefined"
    (dependency-order [{:name :e}
                       {:name :d}
                       {:name :a}
                       {:name :c}
                       {:name :b}])
    => (in-any-order [{:name :a}
                      {:name :b}
                      {:name :c}
                      {:name :d}
                      {:name :e}]))
  (fact "Dependency sort works"
    (dependency-order [{:name :e, :depends #{:b :d}}
                       {:name :d, :depends #{:c}}
                       {:name :a}
                       {:name :c, :depends #{:a :b}}
                       {:name :b, :depends #{:a}}])
    => [{:name :a}
        {:name :b}
        {:name :c}
        {:name :d}
        {:name :e}])
  (fact "Circular dependencies are reported"
    (dependency-order [{:name :a, :depends #{:b}}
                       {:name :b, :depends #{:c}}
                       {:name :c, :depends #{:a}}])
    => (throws-ex-info "interceptors have circular dependency")))
