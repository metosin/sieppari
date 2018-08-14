(ns sieppari.execute
  (:require [sieppari.util :as u]))

(defn- enter [ctx]
  (if-let [interceptor (-> ctx :stack first)]
    (let [ctx ((:enter interceptor) (update ctx :stack next))]
      (if (or (-> ctx :response)
              (-> ctx :error)
              (-> ctx :stack (empty?)))
        ctx
        (-> ctx
            (update ::done conj interceptor)
            (recur))))
    ctx))

(defn- leave [ctx]
  (if-let [interceptor (-> ctx :stack first)]
    (let [ctx (update ctx :stack next)
          stage (if (:error ctx) :error :leave)]
      (recur ((stage interceptor) ctx)))
    ctx))

(defn- swap-direction [ctx]
  (assoc ctx :stack (::done ctx)))

(defn execute [interceptors request]
  (-> {:request request
       :stack (seq interceptors)}
      (enter)
      (swap-direction)
      (leave)
      (u/throw-if-error!)
      :response))
