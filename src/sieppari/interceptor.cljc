(ns sieppari.interceptor
  (:require [sieppari.async :as a]))

(defrecord Interceptor [name enter leave error])

(defprotocol IntoInterceptor
  (into-interceptor [t] "Given a value, produce an Interceptor Record."))

(defn- exception? [e]
  (instance? #?(:clj Exception :cljs js/Error) e))

(defn- set-result [ctx response]
  (if (and (some? response) (a/async? response))
    (a/continue response (partial set-result ctx))
    (assoc ctx
           (if (exception? response) :error :response)
           response)))

(extend-protocol IntoInterceptor
  ;; Map -> Interceptor:
  #?(:clj clojure.lang.IPersistentMap
     :cljs cljs.core.PersistentHashMap)
  (into-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

  #?(:cljs cljs.core.PersistentArrayMap)
  (into-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

  ;; Function -> Handler interceptor:
  #?(:clj clojure.lang.Fn
     :cljs function)
  (into-interceptor [handler]
    (into-interceptor {:enter (fn [ctx]
                                (set-result ctx (handler (:request ctx))))}))

  ;; Vector -> Interceptor, first element is a function to create
  ;; the interceptor, rest are arguments for it:
  #?(:clj clojure.lang.IPersistentVector
     :cljs cljs.core.PersistentVector)
  (into-interceptor [t]
    (into-interceptor (apply (first t) (rest t))))

  ;; Interceptor -> Interceptor, nop:
  Interceptor
  (into-interceptor [t]
    t)

  ;; nil -> nil, nop:
  nil
  (into-interceptor [_]
    nil))
