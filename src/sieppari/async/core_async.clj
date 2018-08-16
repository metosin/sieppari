(ns sieppari.async.core-async
  (:require [clojure.core.async :refer [go <! <!!]]
            [sieppari.async :as sa]))

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (go (f (<! c))))
  (await [c] (<!! c)))
