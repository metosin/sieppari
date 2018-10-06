(ns sieppari.lumo-runner
  (:require [cljs.test :as test :refer-macros [run-tests]]
            [lumo.core :as lumo]
            sieppari.context-test
            sieppari.queue-test
            sieppari.interceptor-test
            sieppari.core-execute-test
            ;; sieppari.promesa-test       ;; not self-host compatible yet
            ;; sieppari.async.promesa-test
            sieppari.native-promise-test
            sieppari.core-async-test
            sieppari.async.core-async-test))

(enable-console-print!)

(defmethod test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (test/successful? m)
    (lumo/exit 1)))

(run-tests
 'sieppari.context-test
 'sieppari.queue-test
 'sieppari.interceptor-test
 'sieppari.core-execute-test
 'sieppari.native-promise-test
 'sieppari.core-async-test
 'sieppari.async.core-async-test)
