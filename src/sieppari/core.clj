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

(defn- leave [^Iterator it ctx]
  (if (a/async? ctx)
    (a/continue ctx (partial leave it))
    (if (.hasNext it)
      (let [stage (if (:error ctx) :error :leave)
            f (-> it .next stage)]
        (recur it (try-f ctx f)))
      ctx)))

(defn- enter [ctx]
  (if (a/async? ctx)
    (a/continue ctx enter)
    (let [queue ^clojure.lang.PersistentQueue (:queue ctx)
          stack (:stack ctx)
          interceptor (peek queue)]
      (cond

        (not interceptor)
        (leave (clojure.lang.RT/iter stack) ctx)

        (:error ctx)
        (leave (clojure.lang.RT/iter stack) ctx)

        :else
        (recur (-> ctx
                   (assoc :queue (pop queue))
                   (assoc :stack (conj stack interceptor))
                   (try-f (:enter interceptor))))))))

(defn- throw-if-error! [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)

(defn- deliver-result [ctx on-complete]
  (if (a/async? ctx)
    (a/continue ctx (fn [ctx] (deliver-result ctx on-complete)))
    (on-complete ctx)))

;;
;; Public API:
;;

(defn execute
  ([interceptors request on-complete]
   (-> (new Context request nil nil (q/into-queue interceptors) nil)
       (enter)
       (deliver-result (comp on-complete :response)))
   nil)
  ([interceptors request]
   (let [p (promise)]
     (-> (new Context request nil nil (q/into-queue interceptors) nil)
         (enter)
         (deliver-result (partial deliver p)))
     (-> (deref p)
         (throw-if-error!)
         :response))))
