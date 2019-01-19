(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [eftest.runner :as eftest]
            [eftest.report.pretty :as pretty]))

(def reset repl/refresh)
(def start (constantly :ok))
(def stop (constantly :ok))


;;
;; Running tests:
;;

(defn run-unit-tests []
  (eftest/run-tests
    (->> ["test" "core/test"]
         (mapcat eftest.runner/find-tests))
    {:multithread?   true
     :test-warn-time 100
     :report         pretty/report}))

(defn run-all-tests []
  (eftest/run-tests
    (->> ["test" "core/test" "test-i" "core/test-i"]
         (mapcat eftest.runner/find-tests))
    {:multithread?   true
     :test-warn-time 1000
     :report         pretty/report}))

