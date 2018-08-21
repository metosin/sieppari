(ns sieppari.queue
  (:require [sieppari.interceptor :as i]))

(defprotocol IntoQueue
  (into-queue [t]))

(extend-protocol IntoQueue
  clojure.lang.PersistentQueue
  (into-queue [t]
    t)

  clojure.lang.ISeq
  (into-queue [t]
    (into clojure.lang.PersistentQueue/EMPTY
          (keep i/into-interceptor)
          t))

  clojure.lang.Seqable
  (into-queue [t]
    (into-queue (seq t)))

  nil
  (into-queue [_]))
