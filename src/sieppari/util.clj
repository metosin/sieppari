(ns sieppari.util)

(defn terminate [ctx response]
  (assoc ctx :response response))

(defn clear-error [ctx]
  (dissoc ctx :exception))
