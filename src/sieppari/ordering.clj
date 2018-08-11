(ns sieppari.ordering)

(defn dependency-order
  "Put given `interceptors` in dependency order. Can also be described as a
  reverse topological sort. Allows interceptors to declare dependencies to
  other interceptors with `:depends` (defaults to `#{}`)."
  [interceptors]
  (let [interceptors-by-name (->> interceptors
                                  (map #(update % :depends set))
                                  (reduce (fn [acc {:as interceptor :keys [name]}]
                                            (when (contains? acc name)
                                              (throw (ex-info (str "multiple interceptors with name: " name) {})))
                                            (assoc acc name interceptor))
                                          {}))]
    (letfn [(toposort [{:keys [name depends] :as interceptor} path colors]
              (case (name colors)
                :white (let [[interceptors* colors]
                             (toposort-seq (map interceptors-by-name depends)
                                           (conj path name)
                                           (assoc colors name :grey))]
                         [(conj interceptors* interceptor)
                          (assoc colors name :black)])
                :grey (throw (ex-info "interceptors have circular dependency"
                                      {:circular-dependency
                                       (drop-while #(not= % name)
                                                   (conj path name))}))
                :black [() colors]))

            (toposort-seq [interceptors path colors]
              (reduce (fn [[interceptors* colors] interceptor]
                        (let [[interceptors** colors] (toposort interceptor path colors)]
                          [(into interceptors* interceptors**) colors]))
                      [[] colors] interceptors))]

      (let [initial-colors (zipmap (map :name interceptors) (repeat :white))]
        (first (toposort-seq interceptors [] initial-colors))))))