(ns sieppari.perf-test
  (:require [criterium.core :as criterium]
            [sieppari.core :as s.c]
            [sieppari.execute :as s.e]
            [io.pedestal.interceptor :as p.i]
            [io.pedestal.interceptor.chain :as p.c]))

(defn make-interceptor [n]
  {:name (keyword (str "interceptor-" n))
   :enter identity
   :leave identity
   :error identity})

(comment
  (let [n 100
        interceptors (map make-interceptor (range n))
        p-chain (->> interceptors
                     (map p.i/interceptor)
                     (doall))
        s-chain (s.c/into-interceptors interceptors identity)
        compiled (s.e/compile-interceptor-chain s-chain)]
    (println "\n\nn =" n)

    (println "pedestal:")
    (criterium/quick-bench
      (-> {}
          (p.c/enqueue p-chain)
          (p.c/execute)))
    ;=> Execution time mean : 63.826026 µs

    (println "sieppari execute:")
    (criterium/quick-bench
      (s.e/execute s-chain {}))
    ;=> Execution time mean : 8.308733 µs

    (println "siepari compiled:")
    (criterium/quick-bench
      (compiled {}))
    ;=> Execution time mean : 2.484586 µs
    ))
