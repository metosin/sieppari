(ns sieppari.util.graphviz
  (:require [ubergraph.core :as uber]))

;; TODO: Add possible other meta information from interceptors to graphs.

(defn interceptors-chain->graph [interceptors]
  (->> interceptors
       (reduce (fn [[acc n] {:keys [name]}]
                 (if name
                   [(conj acc name) n]
                   [(conj acc (->> n (str "anon-") keyword)) (inc n)]))
               [[] 0])
       (first)
       (partition 2 1)
       (map (comp vec reverse))))

(defn interceptors-dependency->graph [interceptors]
  (->> interceptors
       (filter #(contains? % :name))
       (mapcat (fn [{:keys [name depends]}]
                 (map (partial vector name) depends)))))

(defn multidigraph [data]
  (apply uber/multidigraph data))

(defn viz-graph
  ([graph]
   (viz-graph graph {}))
  ([graph opts]
   (uber/viz-graph graph opts)))
