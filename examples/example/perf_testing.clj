(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.execute :as se]
            [sieppari.compile :as sc]
            [sieppari.core-async.compile :as sac]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]
            [clojure.core.async :refer [<!!]]))

(defn run-simple-perf-test [n]
  (let [interceptors (concat (repeatedly n (constantly {:enter identity
                                                        :leave identity
                                                        :error identity}))
                             [identity])
        p-chain (->> interceptors
                     (map pi/interceptor)
                     (doall))
        s-chain (s/into-interceptors interceptors)
        compiled (sc/compile-interceptor-chain s-chain)
        async-compiled (sac/compile-interceptor-chain s-chain)]
    (println "\n\nn =" n)

    #_#_
    (println "\n\npedestal:")
    (criterium/quick-bench
      (-> {}
          (pc/enqueue p-chain)
          (pc/execute)))

    (println "\n\nsieppari execute:")
    (criterium/quick-bench
      (se/execute s-chain {}))

    #_#_
    (println "\n\nsiepari compiled:")
    (criterium/quick-bench
      (compiled {}))

    ;(println "\n\nsiepari async compiled:")
    ;(criterium/quick-bench
    ;  (<!! (async-compiled {})))
    ))

(defn -main [& _]
  (run-simple-perf-test 100))

(comment

  (run-simple-perf-test 100)

  ; Pedestal:
  ;=> Execution time lower quantile : 71.690876 µs
  ;
  ; sieppari execute:
  ;=> Execution time lower quantile : 11.716710 µs
  ;
  ; sieppari compiled:
  ;=> Execution time lower quantile : 3.981034 µs

  )
