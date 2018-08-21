(ns sieppari.queue-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [sieppari.queue :refer [into-queue]])
  (:import (clojure.lang PersistentQueue)
           (sieppari.interceptor Interceptor)))

(deftest into-queue-test
  (fact
    (into-queue [{}]) => PersistentQueue)
  (fact
    (into-queue '({})) => PersistentQueue)
  (fact
    (into-queue (cons {} nil)) => PersistentQueue)
  (let [the-queue (into-queue [{}])]
    (fact
      (into-queue the-queue) => (partial identical? the-queue)))
  (fact
    (into-queue [{}]) => [Interceptor])
  (fact
    (into-queue nil) => nil))
