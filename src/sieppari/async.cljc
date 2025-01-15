(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [sieppari.util :refer [exception?]])
  #?(:clj (:import java.util.concurrent.CompletionStage
                   java.util.concurrent.CompletionException
                   java.util.function.Function)))

(defprotocol AsyncContext
  (async? [t])
  (continue [t f])
  (catch [c f])
  #?(:clj (await [t])))

#?(:clj
   (extend-protocol AsyncContext
     Object
     (async? [_] false)
     (continue [t f] (f t))
     (await [t] t)))

#?(:cljs
   (extend-protocol AsyncContext
     default
     (async? [_] false)
     (continue [t f] (f t))))

#?(:clj
   (extend-protocol AsyncContext
     clojure.lang.IDeref
     (async? [_] true)
     (continue [c f] (future (f @c)))
     (catch [c f] (future (let [c @c]
                            (if (exception? c) (f c) c))))
     (await [c] @c)))

#?(:clj
   (extend-protocol AsyncContext
     CompletionStage
     (async? [_] true)
     (continue [this f]
       (.thenApply ^CompletionStage this
                   ^Function (reify Function
                               (apply [_ v]
                                 (f v)))))

     (catch [this f]
       (letfn [(handler [e]
                 (if (instance? CompletionException e)
                   (f (.getCause ^Exception e))
                   (f e)))]
         (.exceptionally ^CompletionStage this
                         ^Function (reify Function
                                     (apply [_ v]
                                       (handler v))))))

     (await [this]
       (deref this))))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (async? [_] true)
     (continue [t f] (.then t f))
     (catch [t f] (.catch t f))))
