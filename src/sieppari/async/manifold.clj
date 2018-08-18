(ns sieppari.async.manifold
  (:require [sieppari.async :as sa]
            [manifold.deferred :as d]))

;; FIXME: How to handle `on-error` case?

(extend-protocol sa/AsyncContext
  manifold.deferred.Deferred
  (async? [_] true)
  (continue [d f] (d/on-realized d f f))
  (await [d] (deref d)))
