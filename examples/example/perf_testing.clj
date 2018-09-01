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

(comment

  ; 2.7µs
  ; 0.5µs (chain')
  (bench!
    "manifold: identity"
    @(d/chain'
       {}
       identity
       identity
       identity
       identity
       identity
       identity
       identity
       identity
       identity
       identity))

  ; 73µs
  ; 82µs (chain')
  (bench!
    "manifold: future"
    @(d/chain'
       {}
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)
       #(d/future %)))

  ; 3.5µs
  ; 1.5µs (chain')
  (bench!
    "manifold: success-deferred"
    @(d/chain'
       {}
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %)
       #(d/success-deferred %))))

(def sync-interceptor {:enter identity})
(def async-interceptor {:enter #(a/go %)})
(def deferred-interceptor {:enter d/success-deferred})
(def future-interceptor {:enter #(future %)})
(def delay-interceptor {:enter #(delay %)})

(defn create-s-chain [n i]
  (sq/into-queue (concat (repeat n i) [identity])))

(defn create-s-mixed-chain [n i]
  (sq/into-queue (concat (repeat (dec n) identity) [i identity])))

(defn run-simple-perf-test [n]
  (let [sync-interceptors (concat (repeat n sync-interceptor) [identity])
        async-interceptors (concat (repeat n async-interceptor) [identity])

        p-context {:request {}}
        p-sync-chain (mapv pi/interceptor sync-interceptors)
        p-async-chain (map pi/interceptor async-interceptors)

        s-sync-chain (create-s-chain n sync-interceptor)
        s-async-chain (create-s-chain n async-interceptor)

        s-deferred-chain (create-s-chain n deferred-interceptor)
        s-future-chain (create-s-chain n future-interceptor)
        s-delay-chain (create-s-chain n delay-interceptor)]

    (suite (str "queue of " n))

    ;; 8.2µs
    (bench!
      "pedestal: sync"
      (->> p-sync-chain
           (pc/enqueue p-context)
           (pc/execute)
           :response))

    ;; 99µs
    (bench!
      "pedestal: core.async"
      (let [p (promise)]
        (->> (cons (make-capture-result-interceptor p) p-async-chain)
             (pc/enqueue p-context)
             (pc/execute))
        @p))

    ;; 1.3µs
    (bench!
      "sieppari: sync (sync)"
      (s/execute s-sync-chain {}))

    ;; 1.3µs
    (bench!
      "sieppari: sync (async)"
      (let [p (promise)]
        (s/execute s-sync-chain {} p identity)
        @p))

    ;; 61µs
    (bench!
      "sieppari: core.async (sync)"
      (s/execute s-async-chain {}))

    ;; 60µs
    (bench!
      "sieppari: core.async (async)"
      (let [p (promise)]
        (s/execute s-async-chain {} p identity)
        @p))

    ;; 140µs
    (bench!
      "sieppari: future (async)"
      (let [p (promise)]
        (s/execute s-future-chain {} p identity)
        @p))

    ;; 84µs
    (bench!
      "sieppari: delay (async)"
      (let [p (promise)]
        (s/execute s-delay-chain {} p identity)
        @p))

    ;; 84µs
    ;; 62µs (chain'-)
    (bench!
      "sieppari: deferred (sync)"
      (s/execute s-deferred-chain {}))

    ;; 84µs
    ;; 84µs (chain'-)
    (bench!
      "sieppari: deferred (async)"
      (let [p (promise)]
        (s/execute s-deferred-chain {} p identity)
        @p))))

(defn one-async-in-sync-pipeline-test [n]

  (suite (str "homogeneous queue of " n))

  ;; 2µs
  ;; 1.4µs (iterator)
  (let [chain (create-s-chain n identity)]
    (bench!
      "sieppari: identity"
      (let [p (promise)]
        (s/execute chain {} p identity)
        @p)))

  ;; 86µs
  (let [chain (create-s-chain n deferred-interceptor)]
    (bench!
      "sieppari: deferred"
      (let [p (promise)]
        (s/execute chain {} p identity)
        @p)))

  ;; 60µs
  (let [chain (create-s-chain n async-interceptor)]
    (bench!
      "sieppari: core.async"
      (let [p (promise)]
        (s/execute chain {} p identity)
        @p)))


  (suite (str "queue of " (dec n) " sync + 1 async step"))

  ;; 19µs
  (let [chain (create-s-mixed-chain n deferred-interceptor)]
    (bench!
      "sieppari: deferred"
      (let [p (promise)]
        (s/execute chain {} p identity)
        @p)))

  ;; 21µs
  (let [chain (create-s-mixed-chain n async-interceptor)]
    (bench!
      "sieppari: core.async"
      (let [p (promise)]
        (s/execute chain {} p identity)
        @p))))

(defn -main [& _]
  (run-simple-perf-test 10)
  (one-async-in-sync-pipeline-test 10))

(comment
  (run-simple-perf-test 10)
  (one-async-in-sync-pipeline-test 10)
  (-main))
