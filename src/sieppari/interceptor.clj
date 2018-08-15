(ns sieppari.interceptor
  (:require [sieppari.async :as a]))

(defrecord Interceptor [name enter leave error])

(defprotocol IntoInterceptor
  (into-interceptor [t] "Given a value, produce an Interceptor Record."))

(extend-protocol IntoInterceptor
  ; Map -> Interceptor:
  clojure.lang.IPersistentMap
  (into-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

  ; Function -> Handler interceptor:
  clojure.lang.Fn
  (into-interceptor [handler]
    (into-interceptor {:enter (fn [ctx]
                                (let [response (handler (:request ctx))]
                                  (if (a/async? response)
                                    (a/continue response (partial assoc ctx :response))
                                    (assoc ctx :response response))))}))

  ; Vector -> Interceptor, first element is a function to create
  ; the interceptor, rest are arguments for it:
  clojure.lang.IPersistentVector
  (into-interceptor [t]
    (into-interceptor (apply (first t) (rest t))))

  ; Interceptor -> Interceptor, nop:
  Interceptor
  (into-interceptor [t]
    t)

  ; nil -> nil, nop:
  nil
  (into-interceptor [t]
    nil))

(defn into-interceptors
  "Accepts a seq of interceptor maps, Interceptors, nils or
  handler functions, returns a seq of Interceptor records with
  nils removed."
  [interceptors]
  (keep into-interceptor interceptors))
