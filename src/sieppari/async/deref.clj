(ns sieppari.async.deref
  (:require [sieppari.async :as sa]))

(extend-protocol sa/AsyncContext
  clojure.lang.IDeref
  (async? [_] true)
  (continue [c f] (let [p (promise)]
                    (future
                      (deliver p (f @c)))
                    p))
  (await [c] @c))
