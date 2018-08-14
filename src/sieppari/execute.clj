(ns sieppari.execute
  (:require [sieppari.util :as u])
  (:import (clojure.lang PersistentQueue)))

(defrecord Context [request response queue stack])

(defn- leave [ctx stack stage]
  (let [it (clojure.lang.RT/iter stack)]
    (loop [ctx ctx, stage stage]
      (if (.hasNext it)
        (if-let [f (-> it .next stage)]
          (let [ctx (u/try-f ctx f)
                stage (if (:error ctx) :error :leave)]
            (recur ctx stage))
          (recur ctx stage))
        ctx))))

(defn- enter [ctx]
  (let [queue ^clojure.lang.PersistentQueue (:queue ctx)
        stack (:stack ctx)
        error (:error ctx)
        interceptor (peek queue)]
    (cond

      (not interceptor)
      (leave ctx stack :leave)

      error
      (leave (assoc ctx :queue nil) stack :error)

      :else
      (let [queue (pop queue)
            stack (conj stack interceptor)
            f (or (:enter interceptor) identity)
            ctx (-> ctx
                    (assoc :queue queue)
                    (assoc :stack stack)
                    (u/try-f f))]
        (recur ctx)))))

(defn execute [interceptors request]
  (-> (map->Context
        {:request request
         :queue (into PersistentQueue/EMPTY interceptors)})
      (enter)
      (u/throw-if-error!)
      :response))
