(ns sieppari.context
  (:require [sieppari.queue :as q]))

(defn terminate
  "Removes all remaining interceptors from context's execution queue.
  This effectively short-circuits execution of Interceptors' :enter
  functions and begins executing the :leave functions.
  Two arity version allows setting the response at the same call."
  ([ctx]
   (assoc ctx :queue q/empty-queue))
  ([ctx response]
   (-> ctx
       (assoc :queue q/empty-queue)
       (assoc :response response))))

(defn inject
  "Adds interceptor or seq of interceptors to the head of context's execution queue. Creates
  the queue if necessary. Returns updated context."
  [ctx interceptor-or-interceptors]
  (let [interceptors (if (sequential? interceptor-or-interceptors)
                       interceptor-or-interceptors
                       (cons interceptor-or-interceptors nil))]
    (assoc ctx :queue (q/into-queue (concat interceptors (:queue ctx))))))

; TODO: figure out how enqueue should work? Should enqueue add interceptors just
#_
(defn enqueue
  "Adds interceptor or seq of interceptors to the end of context's execution queue. Creates
  the queue if necessary. Returns updated context."
  [ctx interceptor-or-interceptors]
  (let [interceptors (into-interceptors (if (sequential? interceptor-or-interceptors)
                                          interceptor-or-interceptors
                                          [interceptor-or-interceptors]))]
    (update ctx :queue (fnil into PersistentQueue/EMPTY) interceptors)))
