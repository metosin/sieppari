(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [sieppari.util :refer [exception?]]))

(defprotocol AsyncContext
  (continue [t f])
  (catch [c f])
  #?(:clj (await [t])))

(defn async?
  [x]
  (satisfies? AsyncContext x))

#?(:clj
   (extend-protocol AsyncContext
     clojure.lang.IDeref
     (continue [c f] (let [p (promise)]
                       (future (p (f @c)))
                       p))
     (catch [c f] (let [p (promise)]
                    (future (p (let [c @c]
                                 (if (exception? c) (f c) c))))
                    p))
     (await [c] @c)))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (continue [t f] (.then t f))
     (catch [t f] (.catch t f))))
