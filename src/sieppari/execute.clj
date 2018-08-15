(ns sieppari.execute
  (:require [sieppari.util :as u])
  (:import (clojure.lang PersistentQueue)))

(defrecord Context [request response error queue stack])

(defn- leave [ctx stack stage]
  (let [it (clojure.lang.RT/iter stack)]
    (loop [ctx ctx, stage stage]
      (if (.hasNext it)
        (let [ctx (u/try-f ctx (-> it .next stage))]
          (recur ctx (if (:error ctx) :error :leave)))
        ctx))))

(defn- enter [ctx]
  (let [queue ^clojure.lang.PersistentQueue (:queue ctx)
        stack (:stack ctx)
        interceptor (peek queue)]
    (cond

      (not interceptor)
      (leave ctx stack :leave)

      (:error ctx)
      (leave (assoc ctx :queue nil) stack :error)

      :else
      (recur (-> ctx
                 (assoc :queue (pop queue))
                 (assoc :stack (conj stack interceptor))
                 (u/try-f (:enter interceptor)))))))

(defn execute [interceptors request]
  (-> (new Context request nil nil (into PersistentQueue/EMPTY interceptors) nil)
      (enter)
      (u/throw-if-error!)
      :response))
