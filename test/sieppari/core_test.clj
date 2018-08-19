(ns sieppari.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.core :as s]
            [clojure.core.async :as a]
            [manifold.deferred :as md]))

(let [d (md/deferred)]
  (println (type d))
  (md/on-realized d
    (fn [x] (println "success!" x))
    (fn [x] (println "error!" x)))
  (future
    (Thread/sleep 500)
    (md/success! d "Jiihaa")))

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

(def wait-result #'s/await-result)

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
