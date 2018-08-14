(ns sieppari.execute
  (:require [sieppari.util :as u]))

(defn- enter [ctx]
  (if-let [interceptor (-> ctx :stack first)]
    (let [ctx (-> ctx
                  (update :stack next)
                  (u/try-f (:enter interceptor)))]
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
    (-> ctx
        (update :stack next)
        (u/try-f ((if (:error ctx) :error :leave) interceptor))
        (recur))
    ctx))

(defn- swap-direction [ctx]
  (assoc ctx :stack (::done ctx)))

(defn execute [interceptors request]
  (-> {:request request
       :stack (seq interceptors)
       ::done ()}
      (enter)
      (swap-direction)
      (leave)
      (u/throw-if-error!)
      :response))
