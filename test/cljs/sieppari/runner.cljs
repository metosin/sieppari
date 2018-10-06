(ns sieppari.runner
  (:require [cljs.test :as test :refer-macros [run-tests] :refer [report]]
            [figwheel.main.testing :refer-macros [run-tests-async]]
            sieppari.context-test
            sieppari.core-execute-test
            sieppari.promesa-test
            sieppari.async.promesa-test
            sieppari.native-promise-test
            sieppari.core-async-test
            sieppari.async.core-async-test))

;; From https://figwheel.org/docs/testing.html

(defn -main [& args]
  (run-tests-async 10000))
