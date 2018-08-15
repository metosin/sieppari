(ns example.perf-testing
  (:require [criterium.core :as criterium]
            [sieppari.core :as s]
            [sieppari.execute :as se]
            [io.pedestal.interceptor :as pi]
            [io.pedestal.interceptor.chain :as pc]))

(defn run-simple-perf-test [n]
  ; Pedestal requires that at least one of :enter, :leave or :error is defined:
  (let [interceptor {:error identity}
        interceptors (concat (repeat n interceptor)
                             [identity])
        p-chain (->> interceptors
                     (map pi/interceptor)
                     (doall))
        s-chain (s/into-interceptors interceptors)]
    (println "\n\nn =" n)

    (println "\n\npedestal:")
    (criterium/quick-bench
      (-> {}
          (pc/enqueue p-chain)
          (pc/execute)))

    (println "\n\nsieppari execute:")
    (criterium/quick-bench
      (se/execute s-chain {}))
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
