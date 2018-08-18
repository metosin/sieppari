(ns sieppari.async-support
  (:require [sieppari.async :as sa]
            [sieppari.async :as sa]))

; Util to try to require an external library namespace, and if successful, add
; support for it at runtime.

(defmacro when-ns [ns & body]
  `(try
     (eval '(do (require ~ns)
                ~@body))
     (catch Exception ~'_)))

;;
;; Support for core.async:
;;

(when-ns '[clojure.core.async :refer [go <! <!!]]
  (extend-protocol sa/AsyncContext
    clojure.core.async.impl.protocols.Channel
    (async? [_] true)
    (continue [c f] (go (f (<! c))))
    (await [c] (<!! c))))
