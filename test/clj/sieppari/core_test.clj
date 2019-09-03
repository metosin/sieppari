(ns sieppari.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as s]
            [clojure.core.async :as a]))

(def try-f #'s/try-f)

(deftest try-f-test
  (fact
    (try-f {} nil)
    => {})
  (fact
    (try-f {} (fn [ctx] (assoc ctx :foo "bar")))
    => {:foo "bar"})
  (fact
    (try-f {} (fn [_] (throw (ex-info "oh no" {}))))
    => {:error (ex-info "oh no" {})}))

(def await-result #'s/await-result)

(def error (RuntimeException. "kosh"))

(deftest wait-result-core-async-test
  (facts "response"
    (await-result {:response :ctx}) => :ctx
    (await-result (a/go {:response :ctx})) => :ctx
    (await-result (a/go (a/go {:response :ctx}))) => :ctx)
  (facts "error"
    (await-result {:error error}) =throws=> error
    (await-result (a/go {:error error})) =throws=> error
    (await-result (a/go (a/go {:error error}))) =throws=> error))

(deftest wait-result-deref-test
  (facts "response"
    (await-result {:response :ctx}) => :ctx
    (await-result (future {:response :ctx})) => :ctx
    (await-result (future (future {:response :ctx}))) => :ctx)
  (facts "exception"
    (await-result {:error error}) =throws=> error
    (await-result (future {:error error})) =throws=> error
    (await-result (future (future {:error error}))) =throws=> error))

(def deliver-result #'s/deliver-result)

(defn fail! [_] (throw (ex-info "should never get here" {})))

(deftest deliver-result-test
  (let [p (promise)]
    (deliver-result {:response :r
                     :on-complete p
                     :on-error fail!})
    (fact
      @p =eventually=> :r))

  (let [p (promise)]
    (deliver-result {:error (ex-info "oh no" {})
                     :on-complete fail!
                     :on-error p})
    (fact
      @p =eventually=> (ex-info "oh no" {})))

  (let [p (promise)]
    (deliver-result (a/go {:response :r
                           :on-complete p
                           :on-error fail!}))
    (fact
      @p =eventually=> :r))

  (let [p (promise)]
    (deliver-result (a/go {:error (ex-info "oh no" {})
                           :on-complete fail!
                           :on-error p}))
    (fact
      @p =eventually=> (ex-info "oh no" {})))

  (let [p (promise)]
    (deliver-result (future (a/go {:response :r
                                   :on-complete p
                                   :on-error fail!})))
    (fact
      @p =eventually=> :r)))
