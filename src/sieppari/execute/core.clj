(ns sieppari.execute.core)

(defn try-f [f ctx]
  (try
    (f ctx)
    (catch Exception e
      (assoc ctx :error e))))

(defn throw-if-error! [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)
