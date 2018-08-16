(ns sieppari.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as s]
            [sieppari.context :as sc]
            [sieppari.async.core-async]
            [sieppari.async.deref]
            [clojure.core.async :as a]))

(def try-f #'s/try-f)

(deftest try-f-test
  (fact
    (try-f {} nil)
    => (just {}))
  (fact
    (try-f {} (fn [ctx] (assoc ctx :foo "bar")))
    => {:foo "bar"})
  (fact
    (try-f {} (fn [_] (throw (ex-info "oh no" {}))))
    => {:error (throws-ex-info "oh no" {})}))

(def throw-if-error! #'s/throw-if-error!)

(deftest throw-if-error!-test
  (fact
    (throw-if-error! {:response :foo})
    => {:response :foo})
  (fact
    (throw-if-error! {:error (ex-info "oh no" {})})
    => (throws-ex-info "oh no" {})))

(def wait-result #'s/wait-result)

(deftest wait-result-core-async-test
  (fact
    (wait-result :ctx) => :ctx)
  (fact
    (wait-result (a/go :ctx)) => (just :ctx))
  (fact
    (wait-result (a/go (a/go :ctx))) => (just :ctx)))

(deftest wait-result-deref-test
  (fact
    (wait-result :ctx) => :ctx)
  (fact
    (wait-result (future :ctx)) => (just :ctx))
  (fact
    (wait-result (future (future :ctx))) => (just :ctx)))

(def deliver-result #'s/deliver-result)

(defn fail! [_] (throw (ex-info "should never get here" {})))

(deftest deliver-result-test
  (let [p (promise)]
    (deliver-result {:response :r
                     :on-complete (partial deliver p)
                     :on-error fail!})
    (fact {:timeout 10}
      @p => :r))

  (let [p (promise)]
    (deliver-result {:error (ex-info "oh no" {})
                     :on-complete fail!
                     :on-error (partial deliver p)})
    (fact {:timeout 10}
      @p => (throws-ex-info "oh no" {})))

  (let [p (promise)]
    (deliver-result (a/go {:response :r
                           :on-complete (partial deliver p)
                           :on-error fail!}))
    (fact {:timeout 10}
      @p => :r))

  (let [p (promise)]
    (deliver-result (a/go {:error (ex-info "oh no" {})
                           :on-complete fail!
                           :on-error (partial deliver p)}))
    (fact {:timeout 10}
      @p => (throws-ex-info "oh no" {})))

  (let [p (promise)]
    (deliver-result (future (a/go {:response :r
                                   :on-complete (partial deliver p)
                                   :on-error fail!})))
    (fact {:timeout 10}
      @p => :r)))
