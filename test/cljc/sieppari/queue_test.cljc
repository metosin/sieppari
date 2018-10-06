(ns sieppari.queue-test
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
            [sieppari.queue :as sq]
            [sieppari.interceptor :as si])
  #?(:clj (:import (clojure.lang PersistentQueue)
                   (sieppari.interceptor Interceptor))))

(deftest into-queue-test
  (is (instance? #?(:clj PersistentQueue :cljs cljs.core.PersistentQueue)
                 (sq/into-queue [{}])))

  (is (instance? #?(:clj PersistentQueue :cljs cljs.core.PersistentQueue)
                 (sq/into-queue '({}))))

  (is (instance? #?(:clj PersistentQueue :cljs cljs.core.PersistentQueue)
                 (sq/into-queue (cons {} nil))))

  (let [the-queue (sq/into-queue [{}])]
    (is (identical? the-queue (sq/into-queue the-queue))))

  (is (every? (partial instance? #?(:clj Interceptor :cljs si/Interceptor))
              (sq/into-queue [{}])))

  (is (nil? (sq/into-queue [])))

  (is (nil? (sq/into-queue '())))

  (is (nil? (sq/into-queue nil))))
