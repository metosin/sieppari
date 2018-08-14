(ns sieppari.execute
  (:require [sieppari.core]
            [sieppari.util :as u])
  (:import (sieppari.core Interceptor)))

(defn- enter [ctx]
  (if-let [^Interceptor interceptor (-> ctx :stack first)]
    (let [ctx ((.enter interceptor) (update ctx :stack next))]
      (if (or (-> ctx :response)
              (-> ctx :error)
              (-> ctx :stack (empty?)))
        ctx
        (-> ctx
            (update ::done conj interceptor)
            (recur))))
    ctx))

(defn- leave [ctx]
  (if-let [^Interceptor interceptor (-> ctx :stack first)]
    (let [ctx (update ctx :stack next)]
      (recur (if (:error ctx)
               ((.error interceptor) ctx)
               ((.leave interceptor) ctx))))
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
