(ns sieppari.execute.sync-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as sc]
            [sieppari.execute.sync :as ses])
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
                    (vec)))

;;
;; enter:
;;

; Import private sc/enter:
(def enter #'ses/enter)

(deftest enter-test
  (fact "applies interceptors in order"
    (enter {:request 41} test-chain)
    => [{:enter-fn [:a :b :c]}
        ...])

  (fact "applies handler with :request"
    (enter {:request 41} test-chain)
    => [{:response 42}
        ...])

  (fact "the `done` contains applied interceptors in leave order"
    (enter {:request 41} test-chain)
    => [any
        [{:name :c}
         {:name :b}
         {:name :a}]])

  (fact "error in interceptor :c, exception in ctx and done has :b and :a"
    (enter {:request 41} (sc/into-interceptors [(make-test-interceptor :a)
                                                (make-test-interceptor :b)
                                                {:name :c
                                                 :enter (fn [ctx]
                                                          (throw (ex-info "oh no" {})))}
                                                inc]))
    => [{:enter-fn [:a :b]
         :error (ex-info? "oh no")}
        [{:name :b}
         {:name :a}]])

  (fact "error in handler, exception in ctx and done has :c, :b and :a"
    (enter {:request 41} (->> [(make-test-interceptor :a)
                               (make-test-interceptor :b)
                               (make-test-interceptor :c)
                               (fn [request]
                                 (throw (ex-info "oh no" {})))]
                              (sc/into-interceptors)))
    => [{:enter-fn [:a :b :c]
         :error (ex-info? "oh no")}
        [{:name :c}
         {:name :b}
         {:name :a}]]))

;;
;; leave:
;;

; Import private leave:
(def leave #'ses/leave)

; The `done` part of sc/enter response when execution was successful:
(def done-stack [(nth test-chain 2)
                 (nth test-chain 1)
                 (nth test-chain 0)])

(deftest leave-test
  (fact "leave with successful enter response applies all leave functions"
    (leave [{:response 42} done-stack])
    => {:response 42
        :leave-fn [:c :b :a]})

  (fact "failure from handler causes application of all error functions"
    (leave [{:error (ex-info "oh no" {})} done-stack])
    => {:error (ex-info? "oh no")
        :error-fn [:c :b :a]})

  (fact "failure in leave function of interceptor :b, execution moves to error path"
    (leave [{:response 42} (assoc-in done-stack
                                     [1 :leave]
                                     (fn [ctx] (throw (ex-info "oh no" {}))))])
    => {; c: was applies successfully
        :leave-fn [:c]
        ; :b caused an error causing execution to follow error path
        :error-fn [:a]
        ; The error is still in resulting ctx
        :error (ex-info? "oh no")})

  (fact "failure in handler, but :b can correct it"
    (leave [{:error (ex-info "oh no" {})} (assoc-in done-stack
                                                        [1 :error]
                                                        (fn [ctx]
                                                          (-> ctx
                                                              (dissoc :error)
                                                              (assoc :response :fixed-by-b))))])
    => {; c: error was applies
        :error-fn [:c]
        ; :b fixed error, execution moved to leave path
        :leave-fn [:a]
        ; result by :b
        :response :fixed-by-b}))

;;
;; Execute:
;;

(deftest execute-test
  (fact
    (ses/execute test-chain 41) => 42))

