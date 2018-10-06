(ns sieppari.core
  #?(:cljs (:refer-clojure :exclude [iter]))
  (:require [sieppari.queue :as q]
            [sieppari.async :as a]
            #?(:clj [sieppari.async.ext-lib-support]
               :cljs [goog.iter :as iter]))
  #?(:clj (:import (java.util Iterator))))

(defrecord Context [request response error queue stack on-complete on-error])

(defn- try-f [ctx f]
  (if f
    (try
      (f ctx)
      (catch #?(:clj Exception :cljs :default) e
        (assoc ctx :error e)))
    ctx))

(defn- leave [ctx]
  (if (a/async? ctx)
    (a/continue ctx leave)
    (let [it (:stack ctx)]
      (if (.hasNext it)
        (let [stage (if (:error ctx) :error :leave)
              f (-> it .next stage)]
          (recur (try-f ctx f)))
        ctx))))

(defn- iter [v]
  #?(:clj (clojure.lang.RT/iter v)
     :cljs (cljs.core/iter v)))

(defn- enter [ctx]
  (if (a/async? ctx)
    (a/continue ctx enter)
    (let [queue (:queue ctx)
          stack (:stack ctx)
          interceptor (peek queue)]
      (if (or (not interceptor) (:error ctx))
        (assoc ctx :stack (iter stack))
        (recur (-> ctx
                   (assoc :queue (pop queue))
                   (assoc :stack #?(:clj (conj stack interceptor)
                                    :cljs (doto (or stack (array))
                                            (.unshift interceptor))))
                   (try-f (:enter interceptor))))))))

#?(:clj
   (defn- await-result [ctx]
     (if (a/async? ctx)
       (recur (a/await ctx))
       (if-let [error (:error ctx)]
         (throw error)
         (:response ctx)))))

(defn- deliver-result [ctx]
  (if (a/async? ctx)
    (a/continue ctx deliver-result)
    (let [error (:error ctx)
          result (or error (:response ctx))
          callback (if error :on-error :on-complete)
          f (callback ctx identity)]
      (f result))))

(defn- context
  ([request queue]
   (new Context request nil nil queue nil nil nil))
  ([request queue on-complete on-error]
   (new Context request nil nil queue nil on-complete on-error)))

;;
;; Public API:
;;

(defn execute
  ([interceptors request on-complete on-error]
   (if-let [queue (q/into-queue interceptors)]
     (-> (context request queue on-complete on-error)
         (enter)
         (leave)
         (deliver-result))
     ;; It is always necessary to call on-complete or the computation would not
     ;; keep going.
     (on-complete nil))
   nil)
  #?(:clj
     ([interceptors request]
      (if-let [queue (q/into-queue interceptors)]
        (-> (context request queue)
            (enter)
            (leave)
            (await-result))))))
