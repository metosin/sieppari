(ns sieppari.async-test-modules
  (:require [sieppari.async :as sa]
            [clojure.core.async :refer [go <! <!!]]))

;; Can't have dependency to sieppari.async.core-async, that would create
;; circular dependency.

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (go (f (<! c))))
  (await [c] (<!! c)))

(extend-protocol sa/AsyncContext
  clojure.lang.IDeref
  (async? [_] true)
  (continue [c f] (let [p (promise)]
                    (future
                      (deliver p (f @c)))
                    p))
  (await [c] @c))
