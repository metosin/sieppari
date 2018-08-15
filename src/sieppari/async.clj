(ns sieppari.async)

(defprotocol AsyncContext
  (async? [t])
  (continue [t f]))

(extend-protocol AsyncContext
  Object
  (async? [_] false)
  (continue [t f] (f t)))
