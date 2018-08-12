(ns sieppari.execute.sync-compile
  (:require [sieppari.execute.core :as sec]))

(defn try-f [f ctx]
  (try
    (f ctx)
    (catch Exception e
      (assoc ctx :exception e))))

(defn compile-interceptor-chain
  "Waring: experimental"
  [interceptor-chain]
  (let [compile-fn (fn [next-f interceptor]
                     (fn [ctx]
                       (let [ctx (try-f (:enter interceptor) ctx)]
                         (if (or (contains? ctx :response)
                                 (contains? ctx :exception))
                           ctx
                           (let [ctx (next-f ctx)
                                 stage-f (if (contains? ctx :exception)
                                           (:error interceptor)
                                           (:leave interceptor))]
                             (try-f stage-f ctx))))))
        compiled-f (reduce compile-fn
                           identity
                           (reverse interceptor-chain))]
    (fn [request]
      (-> {:request request}
          (compiled-f)
          (sec/throw-if-error!)
          :response))))
