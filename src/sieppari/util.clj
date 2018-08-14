(ns sieppari.util)

(defn try-f [ctx f]
  (if f
    (try
      (f ctx)
      (catch Exception e
        (assoc ctx :error e)))
    ctx))

(defn throw-if-error!
  "If `ctx` has exception under `:error` key, throws that exception. Otherwise
  return the `ctx`."
  [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)
