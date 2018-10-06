(ns sieppari.context-test
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
            [sieppari.context :as sc]
            [sieppari.interceptor :as si]
            [sieppari.queue :as sq]))

(def empty-queue
  #?(:clj clojure.lang.PersistentQueue/EMPTY
     :cljs #queue []))

(deftest terminate-test
  (let [queue (sq/into-queue [{:name :a} {:name :b}])]
    (is (= {:queue empty-queue}
           (sc/terminate {:queue queue})))

    (is (= (sc/terminate {:queue queue} :the-response)
           {:queue empty-queue
            :response :the-response}))))

(deftest inject-test
  (let [queue (sq/into-queue [{:name :a} {:name :b}])]
    (is (= {:queue (conj empty-queue
                         (si/map->Interceptor {:name :x :enter nil :leave nil :error nil})
                         (si/map->Interceptor {:name :a :enter nil :leave nil :error nil})
                         (si/map->Interceptor {:name :b :enter nil :leave nil :error nil}))}
           (sc/inject {:queue queue} {:name :x}))
        "it should add the x interceptor at the head of the queue")

    (is (instance? #?(:clj clojure.lang.PersistentQueue
                      :cljs cljs.core/PersistentQueue)
                   (:queue (sc/inject {:queue queue} {:name :x})))
        "it should return a queue of interceptors")))
