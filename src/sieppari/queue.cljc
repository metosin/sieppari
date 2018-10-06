(ns sieppari.queue
  (:require [sieppari.interceptor :as i]))

(defprotocol IntoQueue
  (into-queue [t]))

(def ^:private empty-queue
  #?(:clj clojure.lang.PersistentQueue/EMPTY
     :cljs #queue []))

(defn- into-queue*
  [t]
  (when (seq t)
    (into empty-queue
          (keep i/into-interceptor)
          t)))

#?(:clj
   (extend-protocol IntoQueue
     clojure.lang.PersistentQueue
     (into-queue [t]
       t)

     clojure.lang.ISeq
     (into-queue [t]
       (into-queue* t))

     clojure.lang.Seqable
     (into-queue [t]
       (into-queue* (seq t)))

     nil
     (into-queue [_])))

#?(:cljs
   (extend-protocol IntoQueue
     cljs.core.PersistentQueue
     (into-queue [t]
       t)

     cljs.core.List
     (into-queue [t]
       (into-queue* t))

     cljs.core.LazySeq
     (into-queue [t]
       (into-queue* t))

     cljs.core.PersistentVector
     (into-queue [t]
       (into-queue* t))

     cljs.core.RSeq
     (into-queue [t]
       (into-queue* t))

     cljs.core.EmptyList
     (into-queue [_])

     cljs.core.Cons
     (into-queue [t]
       (conj empty-queue t))

     array
     (into-queue [t]
       (into-queue* t))

     nil
     (into-queue [_])))
