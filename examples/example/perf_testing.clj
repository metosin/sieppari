(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.queue :as sq]
            [sieppari.async.core-async]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]
            [manifold.deferred :as d]
            [promesa.core :as p]
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
(def promesa-interceptor {:enter p/promise})
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
        s-delay-chain (create-s-chain n delay-interceptor)
        s-promesa-chain (create-s-chain n promesa-interceptor)]

    (suite (str "queue of " n))

    ;; 8.2µs
    ;;baseline 11.03µs
    (bench!
      "pedestal: sync"
      (->> p-sync-chain
           (pc/enqueue p-context)
           (pc/execute)
           :response))

    ;; 99µs
    ;;baseline 124.10µs
    (bench!
      "pedestal: core.async"
      (let [p (promise)]
        (->> (cons (make-capture-result-interceptor p) p-async-chain)
             (pc/enqueue p-context)
             (pc/execute))
        @p))

    ;; 1.3µs
    ; baseline 3.12µs
    ;3.4
    (bench!
      "sieppari: sync (sync)"
      (s/execute s-sync-chain {}))

    ;; 1.3µs
    ;baseline 3.46µs
    ;3.6
    (bench!
      "sieppari: sync (async)"
      (let [p (promise)]
        (s/execute s-sync-chain {} p identity)
        @p))

    ;; 61µs
    ;;baseline  78.13µs
    ;;59.90µs
    (bench!
      "sieppari: core.async (sync)"
      (s/execute s-async-chain {}))

    ;; 60µs
    ;;baseline  89.38µs
    ;;59.18µs
    (bench!
      "sieppari: core.async (async)"
      (let [p (promise)]
        (s/execute s-async-chain {} p identity)
        @p))

    ;; 140µs
    ;baseline 186.33µs
    ;68.83µs
    (bench!
      "sieppari: future (async)"
      (let [p (promise)]
        (s/execute s-future-chain {} p identity)
        @p))

    ;; 84µs
    ;baseline  171.79µs
    ; 27.79µs
    (bench!
      "sieppari: delay (async)"
      (let [p (promise)]
        (s/execute s-delay-chain {} p identity)
        @p))

    ;; 84µs
    ;; 62µs (chain'-)
    ;;baseline   111.90µs
    ;;  28.77µs
    (bench!
      "sieppari: deferred (sync)"
      (s/execute s-deferred-chain {}))

    ;; 84µs
    ;; 84µs (chain'-)
    ;;baseline 131.95µs
    ; 26.16µs
    (bench!
      "sieppari: deferred (async)"
      (let [p (promise)]
        (s/execute s-deferred-chain {} p identity)
        @p))

    ;; 36µs
    ;; 3.8µs
    ;;baseline  5.67µs
    (bench!
      "sieppari: promesa (sync)"
      (s/execute s-promesa-chain {}))

    ;; 38µs
    ;; 4.0µs
    ;baseline 5.13µs
    ;4.72µs
    (bench!
      "sieppari: promesa (async)"
      (let [p (promise)]
        (s/execute s-promesa-chain {} p identity)
        @p))
    ))

(defn one-async-in-sync-pipeline-test [n]

  (doseq [[name chain] [[(str "homogeneous queue of " n) create-s-chain]
                        [(str "queue of " (dec n) " sync + 1 async step") create-s-mixed-chain]]
          :let [_ (suite name)]
          [name interceptor] [["identity" identity]
                              ["deferred" deferred-interceptor]
                              ["core.async" async-interceptor]
                              ["promesa" promesa-interceptor]]]

    (let [interceptors (chain n interceptor)]
      (bench!
        name
        (let [p (promise)]
          (s/execute interceptors {} p identity)
          @p)))

    ;; 1.8µs
    ;; 1.7µs
    ;baseline  5.75µs
    ;6.17µs
    "identity"

    ;; 93µs
    ;; 20µs
    ;baseline  29.13µs
    ;18.25µs
    "deferred"

    ;; 54µs
    ;; 20µs
    ;baseline 32.04µs
    ;22.34µs
    "core.async"

    ;; 40µs => 4.0µs
    ;; 19µs => 2.5µs
    ;baseline 6.02µs
    ; 5.96µs
    "promesa"))

(defn -main [& _]
  (run-simple-perf-test 10)
  (one-async-in-sync-pipeline-test 10))

(comment
  (run-simple-perf-test 10)
  (one-async-in-sync-pipeline-test 10)
  (-main))
