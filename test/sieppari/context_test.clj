(ns sieppari.context-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.queue :as sq]
            [sieppari.context :as sc]))

(deftest terminate-test
  (let [queue (sq/into-queue [{:name :a} {:name :b}])]
    (fact
      (sc/terminate {:queue queue})
      => (just {:queue clojure.lang.PersistentQueue/EMPTY}))
    (fact
      (sc/terminate {:queue queue} :the-response)
      => (just {:queue clojure.lang.PersistentQueue/EMPTY
                :response :the-response}))))

(deftest inject-test
  (let [queue (sq/into-queue [{:name :a} {:name :b}])]
    (fact
      (sc/inject {:queue queue} {:name :x})
      => {:queue [{:name :x}
                  {:name :a}
                  {:name :b}]})
    (fact
      (sc/inject {:queue queue} {:name :x})
      => {:queue clojure.lang.PersistentQueue})))

; TODO: figure out how enqueue should work? Should enqueue add interceptors just
#_(deftest enqueue-test
    (let [queue (into clojure.lang.PersistentQueue/EMPTY (into-interceptors [{:name :a} {:name :b}]))]
      (fact
        (enqueue {:queue queue} {:name :x})
        => {:queue [{:name :a}
                    {:name :b}
                    {:name :x}]})
      (fact
        (enqueue {:queue queue} {:name :x})
        => {:queue clojure.lang.PersistentQueue})))
