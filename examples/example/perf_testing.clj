(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.queue :as sq]
            [sieppari.async.core-async]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]
            [clojure.core.async :as a]))

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

        p-sync-chain (doall (map pi/interceptor sync-interceptors))

        s-sync-chain (sq/into-queue sync-interceptors)
        s-async-chain (sq/into-queue async-interceptors)]
    (println "\n\nn =" n)

    (do (println "\n\npedestal sync:")
        (criterium/quick-bench
          (->> p-sync-chain
               (pc/enqueue {})
               (pc/execute))))

    (do (println "\n\npedestal async:")
        (criterium/quick-bench
          (let [p (promise)
                interceptors (concat [(make-capture-result-interceptor p)]
                                     (repeat 10 async-interceptor)
                                     [identity])]
            (->> (map pi/interceptor interceptors)
                 (pc/enqueue {})
                 (pc/execute))
            @p)))

    (println "\n\nsieppari sync chain, sync execute:")
    (criterium/quick-bench
      (s/execute s-sync-chain {}))

    (println "\n\nsieppari sync chain, async execute:")
    (criterium/quick-bench
      (let [p (promise)]
        (s/execute s-sync-chain {} (partial deliver p) identity)
        @p))

    (println "\n\nsieppari async chain, sync execute:")
    (criterium/quick-bench
      (s/execute s-async-chain {}))

    (println "\n\nsieppari async chain, async execute:")
    (criterium/quick-bench
      (let [p (promise)]
        (s/execute s-async-chain {} (partial deliver p) identity)
        @p))
    ))

(defn -main [& _]
  (run-simple-perf-test 100))
