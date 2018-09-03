(ns sieppari.async
  (:refer-clojure :exclude [await]))

(defprotocol AsyncContext
  (async? [t])
  (continue [t f])
  (await [t]))

(extend-protocol AsyncContext
  Object
  (async? [_] false)
  (continue [t f] (f t))
  (await [t] t)

  clojure.lang.IDeref
  (async? [_] true)
  (continue [c f] (let [p (promise)]
                    (future (p (f @c)))
                    p))
  (await [c] @c))
