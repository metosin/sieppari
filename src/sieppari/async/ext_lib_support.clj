(ns sieppari.async.ext-lib-support)

(defmacro available?
  "Try to require a namespace of external library, return
  `true` if successful."
  [ext-lib-ns]
  `(try
     (require ~ext-lib-ns)
     true
     (catch Exception ~'_
       false)))

; Load support for core-async

(when (available? 'clojure.core.async)
  (require 'sieppari.async.core-async))

; Load support for Manifold

(when (available? 'manifold.deferred)
  (require 'sieppari.async.manifold))

; Load support for Promesa

(when (available? 'promesa.core)
  (require 'sieppari.async.promesa))
