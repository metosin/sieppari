(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await])))

(defprotocol AsyncContext
  (async? [t])
  (continue [t f])
  #?(:clj (await [t])))

#?(:clj
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
     (await [c] @c)))

#?(:cljs
   (extend-protocol AsyncContext
     object
     (async? [_] false)
     (continue [t f] (f t))

     number
     (async? [_] false)
     (continue [t f] (f t))

     string
     (async? [_] false)
     (continue [t f] (f t))

     array
     (async? [_] false)
     (continue [t f] (f t))

     js/Promise
     (async? [_] true)
     (continue [t f] (.then t f))))
