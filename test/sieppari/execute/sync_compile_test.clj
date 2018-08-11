(ns sieppari.execute.sync-compile-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as sc]
            [sieppari.execute.sync-compile :as sesc])
  (:import (clojure.lang ExceptionInfo)))

; Returns predicate that checks that the test result is a
; ExceptionInfo, with optional check for message too.

(defn ex-info?
  ([]
   (fn [actual]
     (instance? ExceptionInfo actual)))
  ([message]
   (fn [actual]
     (and (instance? ExceptionInfo actual)
          (= message (.getMessage ^ExceptionInfo actual))))))

; Make an interceptor with given name, interceptor records
; invocations to ctx for later analysis:

(defn make-test-interceptor [name]
  {:name name
   :enter #(update % :enter-fn (fnil conj []) name)
   :leave #(update % :leave-fn (fnil conj []) name)
   :error #(update % :error-fn (fnil conj []) name)})

; Test stack with three interceptors and a handler that response
; with `(inc request)`:

(def test-chain (-> [(make-test-interceptor :a)
                     (make-test-interceptor :b)
                     (make-test-interceptor :c)
                     inc]
                    (sc/into-interceptors)
                    (sesc/compile-interceptor-chain)))


(deftest compile-interceptor-chain-test
  (fact
    (test-chain 41) => 42))

; TODO: add more tests
