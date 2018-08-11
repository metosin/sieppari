(ns sieppari.execute
  (:require [sieppari.core :as c]))

(defn- try-f [f ctx]
  (try
    (f ctx)
    (catch Exception e
      (assoc ctx :exception e))))

(defn- completed? [ctx]
  (contains? ctx :response))

(defn- error? [ctx]
  (contains? ctx :exception))

(defn- returned-nil [interceptor stage]
  (ex-info (format "%s of interceptor %s returned `nil`"
                   (-> stage name)
                   (-> interceptor :name))
           {:interceptor interceptor
            :stage stage}))

(defn- enter [ctx interceptors]
  (loop [[interceptor & more] interceptors
         ctx ctx
         done ()]
    (if-not interceptor
      [ctx done]
      (let [ctx (-> interceptor :enter (try-f ctx))]
        (cond
          (nil? ctx) (throw (returned-nil interceptor :enter))
          (completed? ctx) [ctx done]
          (error? ctx) [ctx done]
          :else (recur more ctx (cons interceptor done)))))))

(defn- leave [[ctx done]]
  (loop [[interceptor & more] done
         ctx ctx]
    (if-not interceptor
      ctx
      (let [stage (if (error? ctx) :error :leave)
            ctx (-> interceptor stage (try-f ctx))]
        (when (nil? ctx)
          (throw (returned-nil interceptor stage)))
        (recur more ctx)))))

(defn throw-on-error! [{:as ctx :keys [exception]}]
  (when exception
    (throw exception))
  ctx)

(defn execute [interceptors request]
  (-> {:request request}
      (enter interceptors)
      (leave)
      (throw-on-error!)
      :response))

(defn compile-interceptor-chain
  "Waring: experimental"
  [interceptor-chain]
  (let [compile-fn (fn [{:keys [enter leave error]} f]
                     (fn [ctx]
                       (try
                         (let [ctx (enter ctx)]
                           (when (nil? ctx)
                             (throw (ex-info "interceptor enter returned nil" {})))
                           (if (completed? ctx)
                             ctx
                             (let [ctx (f ctx)]
                               (if (error? ctx)
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
          (throw-on-error!)
          :response))))
