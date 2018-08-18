(ns sieppari.async.core-async
  (:require [sieppari.async :as sa]
            [clojure.core.async :refer [go <! <!!]]))

;;
;; Don't require this namespace directly. This is loaded
;; automatically from sieppari.async-support if core.async
;; is in classpath.
;;

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (go (f (<! c))))
  (await [c] (<!! c)))
