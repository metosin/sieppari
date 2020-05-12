(ns sieppari.async.manifold
  (:require [sieppari.async :as sa]
            [manifold.deferred :as d]))

(extend-protocol sa/AsyncContext
  manifold.deferred.Deferred
  (async? [_] true)
  (continue [d old-ctx f]
    (if (d/realized? d)
      (if-some [s (d/success-value d nil)]
        (f s)
        (let [e (d/error-value d nil)]
          (assert e)
          (f (assoc old-ctx :error e))))
      (d/on-realized d
                     #(do #_(tap> %) (f %))
                     #(f (assoc old-ctx :error %)))))
  (await [d] (deref d))
  manifold.deferred.ErrorDeferred
  (async? [_] true)
  (continue [d old-ctx f]
    (if (d/realized? d)
      (if-some [s (d/success-value d nil)]
        (f s)
        (let [e (d/error-value d nil)]
          (assert e)
          (f (assoc old-ctx :error e))))
      (d/on-realized d
                     #(do #_(tap> %) (f %))
                     #(f (assoc old-ctx :error %)))))
  (await [d] (deref d)))
