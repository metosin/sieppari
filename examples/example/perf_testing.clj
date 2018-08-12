(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as sc]
            [sieppari.execute.sync :as ses]
            [sieppari.execute.sync-compile :as sesc]
            [io.pedestal.interceptor :as p.i]
            [io.pedestal.interceptor.chain :as p.c]))

(defn run-simple-perf-test [n]
  (let [interceptors (concat (repeatedly n (constantly {:enter identity
                                                        :leave identity
                                                        :error identity}))
                             [identity])
        p-chain (->> interceptors
                     (map p.i/interceptor)
                     (doall))
        s-chain (sc/into-interceptors interceptors)
        compiled (sesc/compile-interceptor-chain s-chain)]
    (println "\n\nn =" n)

    (println "\n\npedestal:")
    (criterium/quick-bench
      (-> {}
          (p.c/enqueue p-chain)
          (p.c/execute)))

    (println "\n\nsieppari execute:")
    (criterium/quick-bench
      (ses/execute s-chain {}))

    (println "\n\nsiepari compiled:")
    (criterium/quick-bench
      (compiled {}))
    ))

(comment

  (run-simple-perf-test 100)

  ; Pedestal:
  ;=> Execution time lower quantile : 71.690876 µs
  ;
  ; sieppari execute:
  ;=> Execution time lower quantile : 11.716710 µs
  ;
  ; sieppari compiled:
  ;=> Execution time lower quantile : 4.167632 µs

  )
