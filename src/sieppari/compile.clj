(ns sieppari.compile
  (:require [sieppari.core :as sc]
            [sieppari.util :as u])
  (:import (sieppari.core Interceptor)))

(defn compile-interceptor-chain
  "Accepts a chain of interceptors, returns a function that accepts
  the request and produces the response."
  [interceptor-chain]
  (let [compile-fn (fn [next-f ^Interceptor interceptor]
                     (fn [ctx]
                       (let [ctx ((.enter interceptor) ctx)]
                         (if (or (:response ctx)
                                 (:error ctx))
                           ctx
                           (let [ctx (next-f ctx)]
                             (if (:error ctx)
                               ((.error interceptor) ctx)
                               ((.leave interceptor) ctx)))))))
        compiled (->> interceptor-chain
                      (sc/into-interceptors)
                      (reverse)
                      (reduce compile-fn
                              identity))]
    (fn [request]
      (-> {:request request}
          (compiled)
          (u/throw-if-error!)
          :response))))
