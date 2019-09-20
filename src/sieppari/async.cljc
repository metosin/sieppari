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
     (continue [c f] (future (f @c)))
     (catch [c f] (future (let [c @c]
                            (if (exception? c) (f c) c))))
     (await [c] @c)))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (continue [t f] (.then t f))
     (catch [t f] (.catch t f))))
