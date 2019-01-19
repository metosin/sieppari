(ns sieppari.async.promesa
  (:require [sieppari.async :as sa]
            [promesa.core :as p])
  #?(:clj (:import (java.util.concurrent CompletionStage))))

#?(:clj
   (extend-protocol sa/AsyncContext
     CompletionStage
     (continue [this f] (p/chain this f))
     (await [this] (deref this))))

#?(:cljs
   (extend-protocol sa/AsyncContext
     js/Promise
     (continue [this f] (p/chain this f))))
