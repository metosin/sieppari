(ns sieppari.async.core-async
  (:require [sieppari.async :as sa]
            [clojure.core.async :as cca
             #?@(:clj [:refer [go <! <!!]]
                 :cljs [:refer-macros [go]])]))

(extend-protocol sa/AsyncContext
  #?(:clj clojure.core.async.impl.protocols.Channel
     :cljs cljs.core.async.impl.channels/ManyToManyChannel)
  (continue [c f] (go (f (cca/<! c))))
  #?(:clj (await [c] (<!! c))))
