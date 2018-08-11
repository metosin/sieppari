(ns sieppari.execute.sync-compile
  (:require [sieppari.execute.core :as sec]))

(defn compile-interceptor-chain
  "Waring: experimental"
  [interceptor-chain]
  (let [compile-fn (fn [{:keys [enter leave error]} f]
                     (fn [ctx]
                       (try
                         (let [ctx (enter ctx)]
                           (when (nil? ctx)
                             (throw (ex-info "interceptor enter returned nil" {})))
                           (if (contains? ctx :response)
                             ctx
                             (let [ctx (f ctx)]
                               (if (contains? ctx :exception)
                                 (error ctx)
                                 (leave ctx)))))
                         (catch Exception e
                           (-> ctx
                               (assoc :exception e)
                               (error))))))
        compiled-f (loop [f identity
                          [interceptor & more] (reverse interceptor-chain)]
                     (if-not interceptor
                       f
                       (recur (compile-fn interceptor f)
                              more)))]
    (fn [request]
      (-> {:request request}
          (compiled-f)
          (sec/throw-if-error!)
          :response))))
