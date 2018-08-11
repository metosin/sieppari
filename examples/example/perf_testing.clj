(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as sc]
            [sieppari.execute.sync :as ses]
            [sieppari.execute.sync-compile :as sesc]
            [io.pedestal.interceptor :as p.i]
            [io.pedestal.interceptor.chain :as p.c]))

(defn make-interceptor [n]
  {:name (keyword (str "interceptor-" n))
   :enter identity
   :leave identity
   :error identity})

(defn run-simple-perf-test []
  (let [n 100
        interceptors (concat (map make-interceptor (range n))
                             [identity])
        p-chain (->> interceptors
                     (map p.i/interceptor)
                     (doall))
        s-chain (sc/into-interceptors interceptors)
        compiled (sesc/compile-interceptor-chain s-chain)]
    (println "\n\nn =" n)

    (println "pedestal:")
    (criterium/quick-bench
      (-> {}
          (p.c/enqueue p-chain)
          (p.c/execute)))
    ;=> Execution time mean : 74.946559 µs

    (println "sieppari execute:")
    (criterium/quick-bench
      (ses/execute s-chain {}))
    ;=> Execution time mean : 12.419542 µs

    (println "siepari compiled:")
    (criterium/quick-bench
      (compiled {}))
    ;=> Execution time mean : 2.484586 µs
    ))

(comment
  (run-simple-perf-test)
  )