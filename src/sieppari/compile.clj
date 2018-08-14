(ns sieppari.compile
  (:require [sieppari.core :as sc]
            [sieppari.util :as u])
  (:import (sieppari.core Interceptor)))

(defn wrap-try [f]
  (fn [ctx]
    (u/try-f ctx f)))

(defn compile-interceptor-chain
  "Accepts a chain of interceptors, returns a function that accepts
  the request and produces the response."
  [interceptor-chain]
  (let [interceptors (->> interceptor-chain
                          (sc/into-interceptors)
                          (reverse))
        compiled (reduce (fn [next-f ^Interceptor interceptor]
                           (fn [ctx]
                             (let [ctx (u/try-f ctx (:enter interceptor))]
                               (if (or (:response ctx)
                                       (:error ctx))
                                 ctx
                                 (let [ctx (next-f ctx)
                                       stage (if (:error ctx) :error :leave)]
                                   (u/try-f ctx (stage interceptor)))))))
                         (-> interceptors first :enter wrap-try)
                         (-> interceptors next))]
    (fn [request]
      (-> {:request request}
          (compiled)
          (u/throw-if-error!)
          :response))))
