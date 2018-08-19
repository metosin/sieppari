(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.queue :as sq]
            [sieppari.async.core-async]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]
            [manifold.deferred :as d]
            [clojure.core.async :as a]))

(set! *warn-on-reflection* true)

(defn raw-title [color s]
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m"))
  (println (str color s "\u001B[0m"))
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m")))

(def title (partial raw-title "\u001B[35m"))
(def suite (partial raw-title "\u001B[32m"))

(defmacro bench! [name & body]
  `(do
     (title ~name)
     (assert (= ~@body {}))
     (let [{[lower#] :lower-q :as res#} (criterium/quick-benchmark (do ~@body) nil)]
       (println "\u001B[32m\n" (format "%.2fµs" (* 1000000 lower#)) "\u001B[0m")
       (println)
       (criterium/report-result res#))
     (println)))

(defn make-capture-result-interceptor [p]
  (pi/interceptor
    {:leave (fn [ctx]
              (deliver p (:response ctx))
              ctx)}))

(defn run-simple-perf-test [n]
  ; Pedestal requires that at least one of :enter, :leave or :error is defined:
  (let [sync-interceptor {:enter identity}
        async-interceptor {:enter (fn [ctx] (a/go ctx))}

        sync-interceptors (concat (repeat n sync-interceptor) [identity])
        async-interceptors (concat (repeat n {:enter (fn [ctx] (a/go ctx))}) [identity])

        p-context {:request {}}
        p-sync-chain (mapv pi/interceptor sync-interceptors)

        s-sync-chain (sq/into-queue sync-interceptors)
        s-async-chain (sq/into-queue async-interceptors)
        s-manifold-chain (sq/into-queue
                           (concat (repeat n {:enter (fn [ctx] (d/future ctx))}) [identity]))

        s-future-chain (sq/into-queue
                         (concat (repeat n {:enter (fn [ctx] (future ctx))}) [identity]))

        s-delay-chain (sq/into-queue
                        (concat (repeat n {:enter (fn [ctx] (delay ctx))}) [identity]))]

    (println "\n... executing chain of" n "enters\n")

    ;; 8.2µs
    (bench!
      "pedestal: sync"
      (->> p-sync-chain
           (pc/enqueue p-context)
           (pc/execute)
           :response))

    ;; 100µs
    (let [interceptors (map pi/interceptor (concat (repeat 10 async-interceptor) [identity]))]
      (bench!
        "pedestal: core.async"
        (let [p (promise)]
          (->> (cons (make-capture-result-interceptor p) interceptors)
               (pc/enqueue p-context)
               (pc/execute))
          @p)))

    ;; 1.3µs
    (bench!
      "sieppari: sync (sync)"
      (s/execute s-sync-chain {}))

    ;; 1.4µs
    (bench!
      "sieppari: sync (async)"
      (let [p (promise)]
        (s/execute s-sync-chain {} (partial deliver p) identity)
        @p))

    ;; 63µs
    (bench!
      "sieppari: core.async (sync)"
      (s/execute s-async-chain {}))

    ;; 60µs
    (bench!
      "sieppari: core.async (async)"
      (let [p (promise)]
        (s/execute s-async-chain {} (partial deliver p) identity)
        @p))

    ;; 140µs
    (bench!
      "sieppari: future (async)"
      (let [p (promise)]
        (s/execute s-future-chain {} (partial deliver p) identity)
        @p))

    ;; 84µs
    (bench!
      "sieppari: delay (async)"
      (let [p (promise)]
        (s/execute s-delay-chain {} (partial deliver p) identity)
        @p))

    #_(bench!
        "sieppari: deferred (async)"
        (let [p (promise)]
          (s/execute s-manifold-chain {} (partial deliver p) identity)
          @p))
    ))

(defn -main [& _]
  (run-simple-perf-test 10))

(comment
  (-main))
