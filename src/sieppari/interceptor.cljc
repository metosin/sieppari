(ns sieppari.interceptor
  (:require [sieppari.async :as a]
            [sieppari.util :refer [exception?]]))

(defrecord Interceptor [name enter leave error])

(defprotocol IntoInterceptor
  (into-interceptor [t] "Given a value, produce an Interceptor Record."))

(extend-protocol IntoInterceptor
  ;; Map -> Interceptor:
  #?(:clj clojure.lang.IPersistentMap
     :cljs cljs.core.PersistentHashMap)
  (into-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

  #?(:cljs cljs.core.PersistentArrayMap)
  (into-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

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
