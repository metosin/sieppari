(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.queue :as sq]
            [sieppari.async :as sa]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]
            [clojure.core.async :as a]))

(extend-protocol sa/AsyncContext
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c f] (a/go (f (a/<! c)))))

(defn make-capture-result-interceptor [p]
  {:leave (fn [ctx]
            (deliver p (:response ctx))
            ctx)})

(defn run-simple-perf-test [n]
  ; Pedestal requires that at least one of :enter, :leave or :error is defined:
  (let [sync-interceptor {:enter identity}

        async-interceptor {:enter (fn [ctx] (a/go ctx))}

        sync-interceptors (concat (repeat n sync-interceptor)
                                  [identity])

        async-interceptors (concat (repeat n async-interceptor)
                                   [identity])

        s-sync-chain (sq/into-queue sync-interceptors)
        s-async-chain (sq/into-queue async-interceptors)]
    (println "\n\nn =" n)

    (println "\n\npedestal sync:")
    (criterium/quick-bench
      (let [p (promise)
            interceptors (concat [(make-capture-result-interceptor p)]
                                 (repeat 10 sync-interceptor)
                                 [identity])]
        (->> (map pi/interceptor interceptors)
             (pc/enqueue {})
             (pc/execute))
        @p))

    (println "\n\npedestal async:")
    (criterium/quick-bench
      (let [p (promise)
            interceptors (concat [(make-capture-result-interceptor p)]
                                 (repeat 10 async-interceptor)
                                 [identity])]
        (->> (map pi/interceptor interceptors)
             (pc/enqueue {})
             (pc/execute))
        @p))

    (println "\n\nsieppari sync:")
    (criterium/quick-bench
      (s/execute s-sync-chain {}))

    (println "\n\nsieppari async:")
    (criterium/quick-bench
      (s/execute s-async-chain {}))
    ))

(defn run-async-perf-test [n]
  (let [sync-interceptor {:enter identity}
        sync-interceptors (concat (repeat n sync-interceptor)
                                  [identity])
        sync-chain (sq/into-queue sync-interceptors)

        core-async-interceptor {:enter (fn [ctx] (a/go ctx))}
        core-async-interceptors (concat (repeat n core-async-interceptor)
                                        [identity])
        core-async-chain (sq/into-queue core-async-interceptors)]

    (println "n =" n)

    (println "\n\nsync:")
    (criterium/quick-bench
      (s/execute sync-chain {}))

    (println "\n\nasync:")
    (criterium/quick-bench
      (s/execute core-async-chain {}))
    ))

(defn -main [& [test-name count]]
  (run-simple-perf-test 100))

(comment

  (run-async-perf-test 10)

  (def d 20)

  (let [p (promise)
        s (System/nanoTime)]
    (s/execute (concat (repeat d {:enter (fn [ctx] (update ctx :request inc))})
                       [identity])
               0
               (partial deliver p))
    (let [r (deref p 100 :timeout)
          d (- (System/nanoTime) s)]
      [r (format "%.3f µs" (/ d 1000.0))]))

  (let [p (promise)
        s (System/nanoTime)]
    (s/execute (concat (repeat d {:enter (fn [ctx] (a/go (update ctx :request inc)))})
                       [identity])
               0
               (partial deliver p))
    (let [r (deref p 100 :timeout)
          d (- (System/nanoTime) s)]
      [r (format "%.3f µs" (/ d 1000.0))]))

  (let [p (promise)
        s (System/nanoTime)
        r (s/execute (concat (repeat d {:enter (fn [ctx] (a/go (update ctx :request inc)))})
                             [identity])
                     0)
        d (- (System/nanoTime) s)]
    [r (format "%.3f µs" (/ d 1000.0))])

  )

;;
;; How?
;;

(comment

  ; Does P call :error in interceptor if same interceptor :enter fails:
  ; Yes

  (->> [{:name :a
         :error (fn [ctx e] (println ":a error") ctx)}
        {:name :b
         :enter (fn [ctx] (throw (ex-info "oh no" {})))
         :error (fn [ctx e] (println ":b error") (throw e))}]
       (map pi/interceptor)
       (pc/execute {})
       :response)

  ; Prints:
  ;  :b error
  ;  :a error
  ;=> nil

  ; Does P call :leave in interceptor if same interceptor :enter terminates:
  ; Yes

  (->> [{:name :a
         :leave (fn [ctx] (println ":a leave") ctx)}
        {:name :b
         :enter (fn [ctx] (pc/terminate ctx))
         :leave (fn [ctx] (println ":b leave") ctx)}]
       (map pi/interceptor)
       (pc/execute {})
       :response)

  ; Prints:
  ;  :b leave
  ;  :a leave
  ;=> nil

  )

(comment



  (let [p (promise)
        interceptor {:enter (fn [ctx] (a/go (update ctx :request inc)))}
        interceptors (concat [(make-capture-result-interceptor p)]
                             (repeat 10 interceptor)
                             [identity])]
    (->> (map pi/interceptor interceptors)
         (pc/enqueue {:request 0})
         (pc/execute))
    @p)

  (let [
        chain (->> interceptors
                   (map pi/interceptor)
                   (doall))]
    (-> {:request 0}
        (pc/enqueue chain)
        (pc/execute)))
  ;=> {:request 0, :response 0}

  (let [p (promise)
        capture-response {:leave (fn [ctx]
                                   (deliver p (:response ctx))
                                   ctx)}
        interceptor {:enter (fn [ctx]
                              (a/go ctx))}
        interceptors (concat [capture-response]
                             (repeat 3 interceptor)
                             [identity])
        chain (->> interceptors
                   (map pi/interceptor))]
    (-> {:request 0}
        (pc/enqueue chain)
        (pc/execute))
    @p)
  ;=> 0

  )

