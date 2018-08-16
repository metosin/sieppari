(ns user
  (:require [clojure.tools.namespace.repl :as repl]))

(def reset repl/refresh)
(def start (constantly :ok))
(def stop (constantly :ok))
