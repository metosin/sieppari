(ns sieppari.execute.sync-compile
  (:require [sieppari.core :as sc]
            [sieppari.execute.core :as sec])
  (:import (sieppari.core Interceptor)))

(defn wrap-stage-f [f]
  (fn [ctx]
    (try
      (f ctx)
      (catch Exception e
        (assoc ctx :exception e)))))

(defn wrap-interceptor [interceptor]
  (-> interceptor
      (update :enter wrap-stage-f)
      (update :leave wrap-stage-f)
      (update :error wrap-stage-f)))

(defn compile-interceptor-chain
  "Accepts a chain of interceptors, returns a function that accepts
  the request and produces the response."
  [interceptor-chain]
  (let [compile-fn (fn [next-f ^Interceptor interceptor]
                     (fn [ctx]
                       (let [ctx ((.enter interceptor) ctx)]
                         (if (or (contains? ctx :response)
                                 (contains? ctx :exception))
                           ctx
                           (let [ctx (next-f ctx)]
                             (if (contains? ctx :exception)
                               ((.error interceptor) ctx)
                               ((.leave interceptor) ctx)))))))
        compiled (reduce compile-fn
                         identity
                         (->> interceptor-chain
                              (sc/into-interceptors)
                              (map wrap-interceptor)
                              (reverse)))]
    (fn [request]
      (-> {:request request}
          (compiled)
          (sec/throw-if-error!)
          :response))))
