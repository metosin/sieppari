(ns sieppari.core
  (:require [sieppari.queue :as q]
            [sieppari.async :as a]
            [sieppari.async-support])
  (:import (java.util Iterator)))

(defrecord Context [request response error queue stack on-complete on-error])

(defn- try-f [ctx f]
  (if f
    (try
      (f ctx)
      (catch Exception e
        (assoc ctx :error e)))
    ctx))

(defn- leave [ctx]
  (if (a/async? ctx)
    (a/continue ctx leave)
    (let [^Iterator it (:stack ctx)]
      (if (.hasNext it)
        (let [stage (if (:error ctx) :error :leave)
              f (-> it .next stage)]
          (recur (try-f ctx f)))
        ctx))))

(defn- iter ^Iterator [v]
  (clojure.lang.RT/iter v))

(defn- enter [ctx]
  (if (a/async? ctx)
    (a/continue ctx enter)
    (let [queue ^clojure.lang.PersistentQueue (:queue ctx)
          stack (:stack ctx)
          interceptor (peek queue)]
      (if (or (not interceptor)
              (:error ctx))
        (update ctx :stack iter)
        (recur (-> ctx
                   (assoc :queue (pop queue))
                   (assoc :stack (conj stack interceptor))
                   (try-f (:enter interceptor))))))))

(defn- throw-if-error! [ctx]
  (when-let [e (:error ctx)]
    (throw e))
  ctx)

(defn- wait-result [ctx]
  (if (a/async? ctx)
    (recur (a/await ctx))
    ctx))

(defn- deliver-result [ctx]
  (if (a/async? ctx)
    (a/continue ctx deliver-result)
    (let [error (:error ctx)
          result (or error (:response ctx))
          callback (if error :on-error :on-complete)
          f (callback ctx identity)]
      (f result))))

;;
;; Public API:
;;


(defn execute
  ([interceptors request on-complete on-error]
   (-> (new Context request nil nil (q/into-queue interceptors) nil on-complete on-error)
       (enter)
       (leave)
       (deliver-result))
   nil)
  ([interceptors request]
   (-> (new Context request nil nil (q/into-queue interceptors) nil nil nil)
       (enter)
       (leave)
       (wait-result)
       (throw-if-error!)
       :response)))
