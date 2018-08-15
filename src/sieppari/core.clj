(ns sieppari.core
  (:require [sieppari.util :as u]))

(defrecord Interceptor [name enter leave error])

(defprotocol IntoInterceptor
  (-interceptor [t] "Given a value, produce an Interceptor Record."))

(extend-protocol IntoInterceptor
  ; Map -> Interceptor:
  clojure.lang.IPersistentMap
  (-interceptor [interceptor-map]
    (map->Interceptor interceptor-map))

  ; Function -> Handler interceptor:
  clojure.lang.Fn
  (-interceptor [handler]
    (-interceptor {:enter (fn [ctx]
                            (->> (:request ctx)
                                 (handler)
                                 (assoc ctx :response)))}))

  ; Vector -> Interceptor, first element is a function to create
  ; the interceptor, rest are arguments for it:
  clojure.lang.IPersistentVector
  (-interceptor [t]
    (-interceptor (apply (first t) (rest t))))

  ; Interceptor -> Interceptor, nop:
  Interceptor
  (-interceptor [t]
    t)

  ; nil -> nil, nop:
  nil
  (-interceptor [t]
    nil))

;;
;; Public API:
;;

(defn into-interceptor
  "Accepts an interceptor map, Interceptor, nil or
  handler function, returns an Interceptor record."
  [t]
  (-interceptor t))

(defn into-interceptors
  "Accepts a seq of interceptor maps, Interceptors, nils or
  handler functions, returns a seq of Interceptor records with
  nils removed."
  [interceptors]
  (keep into-interceptor interceptors))

;;
;; Stack manipulation helpers:
;;

(defn terminate
  "Removes all remaining interceptors from context's execution queue.
  This effectively short-circuits execution of Interceptors' :enter
  functions and begins executing the :leave functions.
  Two arity version allows setting the response at the same call."
  ([ctx]
    (assoc ctx :queue clojure.lang.PersistentQueue/EMPTY))
  ([ctx response]
    (-> ctx
        (assoc :queue clojure.lang.PersistentQueue/EMPTY)
        (assoc :response response))))

(defn inject
  "Adds interceptor or seq of interceptors to the head of context's execution queue. Creates
  the queue if necessary. Returns updated context."
  [ctx interceptor-or-interceptors]
  (let [interceptors (into-interceptors (if (sequential? interceptor-or-interceptors)
                                          interceptor-or-interceptors
                                          [interceptor-or-interceptors]))
        queue (into clojure.lang.PersistentQueue/EMPTY
                    (concat interceptors
                            (:queue ctx)))]
    (assoc ctx :queue queue)))

; TODO: figure out how enqueue should work? Should enqueue add interceptors just
#_
(defn enqueue
  "Adds interceptor or seq of interceptors to the end of context's execution queue. Creates
  the queue if necessary. Returns updated context."
  [ctx interceptor-or-interceptors]
  (let [interceptors (into-interceptors (if (sequential? interceptor-or-interceptors)
                                          interceptor-or-interceptors
                                          [interceptor-or-interceptors]))]
    (update ctx :queue (fnil into clojure.lang.PersistentQueue/EMPTY) interceptors)))
