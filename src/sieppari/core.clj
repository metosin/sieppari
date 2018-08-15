(ns sieppari.core
  (:require [sieppari.async :as a]
            [sieppari.queue :as q])
  (:import (java.util Iterator)))

(defrecord Context [request response error queue stack])

(defn- try-f [ctx f]
  (if f
    (try
      (f ctx)
      (catch Exception e
        (assoc ctx :error e)))
    ctx))

(defn- throw-if-error! [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)

(defn- finnish [ctx]
  (if (a/async? ctx)
    (a/continue ctx finnish)
    (-> ctx
        (throw-if-error!)
        :response)))

(defn- leave [stage ^Iterator it ctx]
  (if (a/async? ctx)
    (a/continue ctx (partial leave stage it))
    (if (.hasNext it)
      (let [ctx (try-f ctx (-> it .next stage))]
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
                   (try-f (:enter interceptor))))))))

;;
;; Public API:
;;

(defn execute
  ([interceptors request]
   (let [response (promise)]
     (-> (new Context request nil nil (q/into-queue interceptors) nil)
         (enter)
         (a/continue (partial deliver response)))
     (deref response)))
  ([interceptors request on-complete]
   (-> (new Context request nil nil (q/into-queue interceptors) nil)
       (enter)
       (a/continue on-complete))
   nil))
