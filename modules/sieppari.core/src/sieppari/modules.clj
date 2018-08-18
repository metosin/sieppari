(ns sieppari.modules
  (:require [sieppari.async :as sa]
            [sieppari.async :as sa]))

;;
;; Support for Clojure deferrables, like `promise` and `future`:
;;

(extend-protocol sa/AsyncContext
  clojure.lang.IDeref
  (async? [_] true)
  (continue [c f] (let [p (promise)]
                    (future
                      (deliver p (f @c)))
                    p))
  (await [c] @c))

; Util, try to require an external library namespace, and if successfull, add
; support for it at runtime.

(defmacro when-ns [ns & body]
  `(try
     (do (require (quote ~ns))
         ~@body)
     (catch Exception ~'_)))

;;
;; Support for core.async:
;;


(when-ns [clojure.core.async :refer [go <! <!!]]
  (extend-protocol sa/AsyncContext
    clojure.core.async.impl.protocols.Channel
    (async? [_] true)
    (continue [c f] (go (f (<! c))))
    (await [c] (<!! c))))
