(ns sieppari.modules)

;;
;; Add-on modules. Try to require add-on modules. Loads the add-on module
;; if it is in classpath.
;;
;; This means that you can enable add-on my just including the add-on module
;; to project dependency.
;;

(def modules '[sieppari.async.core-async
               sieppari.async.deref])

(defn try-require [module-ns]
  (try
    (require module-ns)
    (catch Exception _)))

(doseq [ns modules]
  (try-require ns))
