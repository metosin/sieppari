(ns sieppari.async.core-async
  (:require [sieppari.async :as sa]
            [clojure.core.async :refer [go <! <!!]]))

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (go (f (<! c))))
  (await [c] (<!! c)))
