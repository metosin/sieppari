(ns sieppari.core
  #?(:cljs (:refer-clojure :exclude [iter]))
  (:require [sieppari.queue :as q]
            [sieppari.async :as a]
            [sieppari.util :refer [exception?]]
            [sieppari.interceptor :as inter]
            #?(:cljs [goog.iter :as iter]))
  #?(:clj (:import (java.util Iterator))))

(defrecord Context [error queue stack on-complete on-error async go-async final-transform])

(deftype box [a])
(defn box? [a]
  (instance? box a))

(defn unbox [a]
  (if (box? a)
    (.-a a)
    a))

(defn context? [x]
  (instance? Context x))

(declare enter-leave handle-async)

(defn- try-f [ctx f stage]
  (if f
    (try
      (let [ctx* (f ctx)]
        (if (a/async? ctx*)
          (handle-async ctx* ctx stage)
          (unbox ctx*)))
      (catch #?(:clj Exception :cljs :default) e
        #_(tap> :error)
        #_(tap> (pr-str e))
        (assoc ctx :error e)))
    ctx))

(defn finish [ctx]
  (if (context? ctx)
    (let [error (:error ctx)]
      (if error
        (do
          (when-some [on-error (:on-error ctx )]
            (on-error ctx))
            ctx)
        (let [final-transform (:final-transform ctx identity)
              final-value (final-transform ctx)]
          (when-some [on-complete (:on-complete ctx )]
            (on-complete final-value))
          final-value)))
    ctx))

(defn- leave [ctx]
  (if (context? ctx)
    (let [^Iterator it (:stack ctx)]
      (if (.hasNext it)
        (let [stage (if (:error ctx) :error :leave)
              f     (-> it .next stage)]
          (recur (try-f ctx f leave)))
        (finish ctx)))
    ctx))

(defn- iter [v]
  #?(:clj  (clojure.lang.RT/iter v)
     :cljs (cljs.core/iter v)))

(defn- enter [ctx]
  (if (context? ctx)
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
                   (try-f (:enter interceptor) enter-leave)))))
    (do
      #_(tap> :nocontext-enter)
      #_(tap> ctx)
      #_(tap> :nocontext-enter-out)
      ctx)))

#?(:clj
   (defn- await-result [ctx]
     #_(tap> :await)
     #_(tap> (a/async? ctx))
     #_(tap> ctx)
     (if (a/async? ctx)
       (recur (a/await ctx) )
       (if-let [error (:error ctx)]
         (throw error)
         ctx))))

(defn enter-leave [ctx]
  #_(tap> :enterleave)
  ;#_(tap> ctx)
  (-> ctx
      enter
      ;(doto tap>)
      leave))
(add-tap prn)
(defn handle-async [ctx old-ctx stage]
  (if (:async old-ctx)
    (do (a/continue ctx old-ctx stage)
        nil)
    (if-some [go-async (:go-async old-ctx)]
      (let [async-data (go-async stage)
            go-async!  (nth async-data 1)]
        (a/continue ctx old-ctx go-async!)
        (nth async-data 0))
      (do (a/continue ctx old-ctx stage)
          nil))))

(defn wrap-callback [f p]
  (if f
    (fn [r] #_(tap> :deliver)  (clojure.core/deliver p r) (f r))
    (fn [r] #_(tap> :deliver) (clojure.core/deliver p r))))
#?(:clj
   (defn- go-async-promise [stage]
     (let [p (promise)]
       [p (fn [ctx]
            #_(tap> :go-async-promise)
            #_(tap> ctx)
            #_(tap> stage)
            (-> ctx
                (assoc :on-complete
                       (wrap-callback (:on-complete ctx) p)
                       :on-error
                       (wrap-callback (:on-error ctx) p)
                       :go-async nil
                       :async true)
                stage))])))

(defn- context [m]
  (map->Context m))

(defn- remove-context-keys [ctx]
  (dissoc ctx :error :queue :stack :on-complete :on-error
          :async  :go-async :final-transform))

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
     (-> (assoc ctx :queue queue :on-complete on-complete :on-error on-error
                    :final-transform get-result)
         (context)
         (enter-leave))
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
        (-> (assoc ctx :queue queue :final-transform get-result :go-async go-async-promise)
            (context)
            (enter-leave)
            (await-result))))))

(defn execute
  {:arglists
   '([interceptors request]
     [interceptors request on-complete on-error])}
  ([interceptors request on-complete on-error]
   (execute-context interceptors {:request request} on-complete on-error :response))
  #?(:clj
     ([interceptors request]
      (execute-context interceptors {:request request} :response))))


(defn- async-set-result [ctx continue]
  (fn [response]
    #_(tap> :async-response)
    #_(tap> response)
    #_(tap> ctx)
    (continue
      (if (context? response)
        response
        (assoc ctx
          :response
          response)))))

(defn handle-async-handler [response old-ctx stage]
  (if (:async old-ctx)
    (do (a/continue response old-ctx (async-set-result old-ctx stage))
        nil)
    (if-some [go-async (:go-async old-ctx)]
      (let [async-data (go-async stage)
            go-async!  (nth async-data 1)]
        (a/continue response old-ctx (async-set-result old-ctx go-async!))
        (->box (nth async-data 0)))
      (do (a/continue response old-ctx (async-set-result old-ctx stage))
          nil))))

(defn- set-result [ctx response]
  #_(tap> :response)
  #_(tap> response)
  ;#_(tap> ctx)
  (if (and (some? response) (a/async? response))
    (handle-async-handler response ctx enter-leave)
    (assoc ctx
      (if (exception? response) :error :response)
      response)))
(extend-protocol inter/IntoInterceptor
  ;; Function -> Handler interceptor:
  #?(:clj clojure.lang.Fn
     :cljs function)
  (into-interceptor [handler]
    (inter/into-interceptor {:enter (fn [ctx]
                                #_(tap> :enter-handler)
                                (set-result ctx (handler (:request ctx))))})))
