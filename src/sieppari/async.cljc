(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [sieppari.util :refer [exception?]])
  #?(:clj (:import java.util.concurrent.CompletionStage
                   java.util.concurrent.CompletionException
                   java.util.function.Function)))

(defprotocol AsyncContext
  (async? [t])
  (continue [t old-ctx f])
  #?(:clj (await [t])))

#?(:clj
   (deftype FunctionWrapper [f]
     Function
     (apply [_ v]
       (f v))))

#?(:clj
   (extend-protocol AsyncContext
     Object
     (async? [_] false)
     (continue [t old-ctx f] (f t))
     (await [t] t)))

#?(:cljs
   (extend-protocol AsyncContext
     default
     (async? [_] false)
     (continue [t old-ctx f] (f t))))

(extend-protocol AsyncContext
   nil
  (async? [_] false))

#?(:clj
   (extend-protocol AsyncContext
     clojure.lang.IDeref
     (async? [_] true)
     (continue [c old-ctx f]
       (future
         (let [c (try @c
                      (catch Exception e
                        (assoc old-ctx :error e)))]
           (f c))))
     (await [c] @c)))

#?(:clj
   (extend-protocol AsyncContext
     CompletionStage
     (async? [_] true)
     (continue [this old-ctx f]
       (letfn [(handler [e]
                 (if (instance? CompletionException e)
                   (f (assoc old-ctx :error (.getCause ^Exception e)))
                   (f (assoc old-ctx :error e))))]
         (.exceptionally ^CompletionStage this
                         ^Function (->FunctionWrapper handler)))
       (.thenApply ^CompletionStage this
                   ^Function (->FunctionWrapper f)))
     (await [this]
       (deref this))))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (async? [_] true)
     (continue [t old-ctx f]
       (.catch t (fn [e] (f (assoc old-ctx :error e))))
       (.then t f))))
