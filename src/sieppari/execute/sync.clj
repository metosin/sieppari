(ns sieppari.execute.sync
  (:require [sieppari.execute.core :as ec]))

(defn- enter [ctx interceptors]
  (loop [ctx ctx
         [interceptor & more] interceptors
         done ()]
    (if interceptor
      (let [ctx (ec/try-f (:enter interceptor) ctx)]
        (if (or (contains? ctx :response)
                (contains? ctx :exception))
          [ctx done]
          (recur ctx
                 more
                 (cons interceptor done))))
      [ctx done])))

(defn- leave [[ctx done]]
  (loop [ctx ctx
         [interceptor & more] done]
    (if interceptor
      (let [stage (if (contains? ctx :exception) :error :leave)
            ctx (ec/try-f (stage interceptor) ctx)]
        (recur ctx more))
      ctx)))

(defn execute [interceptors request]
  (-> {:request request}
      (enter interceptors)
      (leave)
      (ec/throw-if-error!)
      :response))
