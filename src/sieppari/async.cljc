(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [sieppari.util :refer [exception?]])
  #?(:clj (:import java.util.concurrent.CompletionStage
                   java.util.concurrent.CompletionException
                   java.util.function.Function)))

(defprotocol AsyncContext
  (continue [t f])
  (catch [c f])
  #?(:clj (await [t])))

(defn async?
  [x]
  (satisfies? AsyncContext x))

#?(:clj
   (deftype FunctionWrapper [f]
     Function
     (apply [_ v]
       (f v))))

#?(:clj
   (extend-protocol AsyncContext
     clojure.lang.IDeref
     (continue [c f] (future (f @c)))
     (catch [c f] (future (let [c @c]
                            (if (exception? c) (f c) c))))
     (await [c] @c)))

#?(:clj
   (extend-protocol AsyncContext
     CompletionStage
     (continue [this f]
       (.thenApply ^CompletionStage this
                   ^Function (->FunctionWrapper f)))

     (catch [this f]
       (letfn [(handler [e]
                  (if (instance? CompletionException e)
                   (f (.getCause ^Exception e))
                   (f e)))]
         (.exceptionally ^CompletionStage this
                         ^Function (->FunctionWrapper handler))))

     (await [this]
       (deref this))))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (continue [t f] (.then t f))
     (catch [t f] (.catch t f))))
