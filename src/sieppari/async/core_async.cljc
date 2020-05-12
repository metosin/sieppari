(ns sieppari.async.core-async
  (:require [sieppari.async :as sa]
            [sieppari.util :refer [exception?]]
            [clojure.core.async :as cca
             #?@(:clj [:refer [go <! <!!]]
                 :cljs [:refer-macros [go]])]))

(extend-protocol sa/AsyncContext
  #?(:clj clojure.core.async.impl.protocols.Channel
     :cljs cljs.core.async.impl.channels/ManyToManyChannel)
  (async? [_] true)
  (continue [c old-ctx f]
    (cca/take! c
               (fn [x]
                 (if (exception? x)
                   (f (assoc old-ctx :error x))
                   (f x)))))
  #?(:clj (await [c] (<!! c))))
