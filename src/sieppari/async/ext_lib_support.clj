(ns sieppari.async.ext-lib-support
  (:require [sieppari.async :as sa]))

(defmacro available?
  "Try to require a namespace of external library, return
  `true` if successful."
  [ext-lib-ns]
  `(try
     (require ~ext-lib-ns)
     true
     (catch Exception ~'_
       false)))

; Load support for core-async:

(when (available? 'clojure.core.async)
  (require 'sieppari.async.core-async))

; Load support for Manifold:

(when (available? 'manifold.deferred)
  (require 'sieppari.async.manifold))
