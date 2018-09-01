(ns sieppari.async.promesa
  (:require [sieppari.async :as sa]
            [promesa.core :as p])
  (:import (java.util.concurrent CompletionStage)))

(extend-protocol sa/AsyncContext
  CompletionStage
  (async? [_] true)
  (continue [this f] (p/chain this f))
  (await [this] (deref this)))
