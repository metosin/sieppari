(ns sieppari.core
  #?(:cljs (:refer-clojure :exclude [iter]))
  (:require [sieppari.queue :as q]
            [sieppari.async :as a]
            #?(:cljs [goog.iter :as iter]))
  #?(:clj (:import (java.util Iterator))))

(defrecord Context [error queue stack on-complete on-error])

(defn- try-f [ctx f]
  (if f
    (try
      (let [ctx* (f ctx)]
        (if (a/async? ctx*)
          (a/catch ctx* (fn [e] (assoc ctx :error e)))
          ctx*))
      (catch #?(:clj Exception :cljs :default) e
        (assoc ctx :error e)))
    ctx))

(defn- leave [ctx]
  (if (a/async? ctx)
    (a/continue ctx leave)
    (let [it (:stack ctx)]
      (if (.hasNext it)
        (let [stage (if (:error ctx) :error :leave)
              f     (-> it .next stage)]
          (recur (try-f ctx f)))
        ctx))))

(defn- iter [v]
  #?(:clj  (clojure.lang.RT/iter v)
     :cljs (cljs.core/iter v)))

(defn- enter [ctx]
  (if (a/async? ctx)
    (a/continue ctx enter)
    (let [queue       (:queue ctx)
          stack       (:stack ctx)
          interceptor (peek queue)]
      (if (or (not interceptor) (:error ctx))
        (assoc ctx :stack (iter stack))
        (recur (-> ctx
                   (assoc :queue (pop queue))
                   (assoc :stack #?(:clj  (conj stack interceptor)
                                    :cljs (doto (or stack (array))
                                            (.unshift interceptor))))
                   (try-f (:enter interceptor))))))))

#?(:clj
   (defn- await-result [ctx get-result]
     (if (a/async? ctx)
       (recur (a/await ctx) get-result)
       (if-let [error (:error ctx)]
         (throw error)
         (get-result ctx)))))

(defn- deliver-result [ctx get-result]
  (if (a/async? ctx)
    (a/continue ctx #(deliver-result % get-result))
    (let [error    (:error ctx)
          result   (or error (get-result ctx))
          callback (if error :on-error :on-complete)
          f        (callback ctx identity)]
      (f result))))

(defn- context [m]
  (map->Context m))

(defn- remove-context-keys [ctx]
  (dissoc ctx :error :queue :stack :on-complete :on-error))

;;
;; Public API:
;;

(defn execute-context
  {:arglists
   '([interceptors ctx]
     [interceptors ctx on-complete on-error])}
  ([interceptors ctx on-complete on-error]
   (execute-context interceptors ctx on-complete on-error remove-context-keys))
  ([interceptors ctx on-complete on-error get-result]
   (if-let [queue (q/into-queue interceptors)]
     (-> (assoc ctx :queue queue :on-complete on-complete :on-error on-error)
         (context)
         (enter)
         (leave)
         (deliver-result get-result))
     ;; It is always necessary to call on-complete or the computation would not
     ;; keep going.
     (on-complete nil))
   nil)
  #?(:clj
     ([interceptors ctx]
      (execute-context interceptors ctx remove-context-keys)))
  #?(:clj
     ([interceptors ctx get-result]
      (when-let [queue (q/into-queue interceptors)]
        (-> (assoc ctx :queue queue)
            (context)
            (enter)
            (leave)
            (await-result get-result))))))

(defn execute
  {:arglists
   '([interceptors request]
     [interceptors request on-complete on-error])}
  ([interceptors request on-complete on-error]
   (execute-context interceptors {:request request} on-complete on-error :response))
  #?(:clj
     ([interceptors request]
      (execute-context interceptors {:request request} :response))))
