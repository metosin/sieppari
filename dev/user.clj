(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [kaocha.repl :as kaocha]))

(def reset repl/refresh)
(def start (constantly :ok))
(def stop (constantly :ok))
