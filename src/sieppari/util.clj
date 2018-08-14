(ns sieppari.util)

(defn wrap-f
  "Given a function, returns a new function that accepts a `ctx` and calls the
  given function `f` with it and returns what the `f` returns. If the call to `f`
  causes an exception, returns `ctx` with the exception under key `:error`.
  If `f` is `nil`, returns `clojure.core/identity`."
  [f]
  (when-not (or (nil? f)
                (fn? f))
    (throw (ex-info "interceptor :enter, :leave and :error must all be functions" {:f f})))
  (if f
    (fn [ctx]
      (try
        (f ctx)
        (catch Exception e
          (assoc ctx :error e))))
    identity))

(defn wrap-interceptor-fns
  "Accepts an interceptor map, wraps `:enter`, `:leave` and `:error` functions
  with `wrap-f`."
  [interceptor-map]
  (reduce (fn [m k]
            (update m k wrap-f))
          interceptor-map
          [:enter :leave :error]))

(defn throw-if-error!
  "If `ctx` has exception under `:error` key, throws that exception. Otherwise
  return the `ctx`."
  [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)
