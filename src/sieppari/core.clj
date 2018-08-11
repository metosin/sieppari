(ns sieppari.core)

(defrecord Interceptor [name enter leave error])

(def ^:private interceptor-defaults {:enter identity
                                     :leave identity
                                     :error identity})

(defprotocol IntoInterceptor
  (-interceptor [t] "Given a value, produce an Interceptor Record."))

(extend-protocol IntoInterceptor
  clojure.lang.IPersistentMap
  (-interceptor [t]
    (let [m (merge interceptor-defaults t)]
      (when-not (every? (comp fn? m) [:enter :leave :error])
        (throw (ex-info "interceptor :enter, :leave and :error must all be functions" {:interceptor m})))
      (map->Interceptor m)))

  clojure.lang.Fn
  (-interceptor [handler]
    (-interceptor (assoc interceptor-defaults
                    :enter (fn [ctx]
                             (->> (handler (:request ctx))
                                  (assoc ctx :response))))))

  Interceptor
  (-interceptor [t]
    t)

  nil
  (-interceptor [t]
    nil))

;;
;; Public API:
;;

(defn into-interceptors
  "Accepts a seq of interceptor maps, Interceptors, nils or
  handler functions, returns a seq of Interceptor records with
  nils removed."
  [interceptors]
  (keep -interceptor interceptors))
