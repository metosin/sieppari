(ns sieppari.execute.core)

(defn try-f [f ctx]
  (try
    (f ctx)
    (catch Exception e
      (assoc ctx :exception e))))

(defn throw-if-error! [ctx]
  (when-let [e (:exception ctx)]
    (throw e))
  ctx)
