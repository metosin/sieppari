(ns sieppari.execute.core-async-compile
  (:require [sieppari.core :as sc]
            [sieppari.execute.core :as sec]
            [clojure.core.async :as a :refer [go <!]])
  (:import (sieppari.core Interceptor)
           (clojure.core.async.impl.protocols Channel)))

(defn wrap-stage-f [f]
  (fn [ctx]
    (go
      (try
        (let [ctx (f ctx)]
          (if (instance? Channel ctx)
            (<! ctx)
            ctx))
        (catch Exception e
          (assoc ctx :error e))))))

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
                       (go
                         (let [ch ((.enter interceptor) ctx)
                               ctx (<! ch)]
                           (if (or (contains? ctx :response)
                                   (contains? ctx :error))
                             ctx
                             (let [ch (next-f ctx)
                                   ctx (<! ch)
                                   ch (if (contains? ctx :error)
                                        ((.error interceptor) ctx)
                                        ((.leave interceptor) ctx))]
                               (<! ch)))))))
        compiled (reduce compile-fn
                         identity
                         (->> interceptor-chain
                              (sc/into-interceptors)
                              (map wrap-interceptor)
                              (reverse)))]
    (fn [request]
      (go (-> {:request request}
              (compiled)
              (<!)
              (some [:error :response]))))))
