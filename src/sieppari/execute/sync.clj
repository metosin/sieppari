(ns sieppari.execute.sync
  (:require [sieppari.execute.core :as ec]))

; TODO: Migrate ec/try-f to this
(defn try-f [ctx stage interceptor]
  (try
    (let [f (stage interceptor)]
      (f ctx))
    (catch Exception e
      (assoc ctx :error e))))

(defn- enter [ctx]
  (if-let [interceptor (-> ctx :stack first)]
    (let [ctx (-> ctx
                  (update :stack next)
                  (try-f :enter interceptor))]
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
        (try-f (if (:error ctx) :error :leave) interceptor)
        (recur))
    ctx))

(defn swap-direction [ctx]
  (assoc ctx :stack (::done ctx)))

(defn execute [interceptors request]
  (-> {:request request
       :stack (seq interceptors)}
      (enter)
      (swap-direction)
      (leave)
      (ec/throw-if-error!)
      :response))
