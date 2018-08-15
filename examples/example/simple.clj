(ns example.simple
  (:require [sieppari.chain :as s]))

(def times-2-interceptor
  {:enter (fn [ctx] (update-in ctx [:request :x] * 2))})

(def inc-interceptor
  {:enter (fn [ctx] (update-in ctx [:request :x] inc))})

;; Simple handler, take `:x` from request, apply `inc`, and
;; return an map with `:y`.

(defn handler [request]
  {:y (inc (:x request))})

(def chain [times-2-interceptor
            inc-interceptor
            handler])

(s/execute chain {:x 20})
;=> {:y 42}
