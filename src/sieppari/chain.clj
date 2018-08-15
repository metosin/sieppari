(ns sieppari.chain
  (:require [sieppari.util :as u]
            [sieppari.async :as a]
            [sieppari.queue :as q])
  (:import (java.util Iterator)))

(defrecord Context [request response error queue stack])

(defn- finnish [ctx]
  (if (a/async? ctx)
    (a/continue ctx finnish)
    (-> ctx
        (u/throw-if-error!)
        :response)))

(defn- leave [stage ^Iterator it ctx]
  (if (a/async? ctx)
    (a/continue ctx (partial leave stage it))
    (if (.hasNext it)
      (let [ctx (u/try-f ctx (-> it .next stage))]
        (recur (if (:error ctx) :error :leave) it ctx))
      (finnish ctx))))

(defn- enter [ctx]
  (if (a/async? ctx)
    (a/continue ctx enter)
    (let [queue ^clojure.lang.PersistentQueue (:queue ctx)
          stack (:stack ctx)
          interceptor (peek queue)]
      (cond

        (not interceptor)
        (leave :leave (clojure.lang.RT/iter stack) ctx)

        (:error ctx)
        (leave :error (clojure.lang.RT/iter stack) ctx)

        :else
        (recur (-> ctx
                   (assoc :queue (pop queue))
                   (assoc :stack (conj stack interceptor))
                   (u/try-f (:enter interceptor))))))))

;;
;; Public API:
;;

(defn execute
  ([interceptors request]
   (-> (new Context request nil nil (q/into-queue interceptors) nil)
       (enter)
       (a/await)))
  ([interceptors request on-complete]
   (-> (new Context request nil nil (q/into-queue interceptors) nil)
       (enter)
       (a/continue on-complete))
    nil))
