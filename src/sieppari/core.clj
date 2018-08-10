(ns sieppari.core)

(defrecord Interceptor [name
                        enter
                        leave
                        error
                        applies?
                        system?
                        depends])

(def interceptor-defaults {:enter identity
                           :leave identity
                           :error identity
                           :applies-to? (constantly true)
                           :system? false
                           :depends #{}})

(defn validate-interceptor [interceptor]
  (when-not (contains? interceptor :name)
    (throw (ex-info "interceptor :name is mandatory" {})))
  interceptor)

(defprotocol IntoInterceptor
  (-interceptor [t] "Given a value, produce an Interceptor Record."))

(extend-protocol IntoInterceptor
  clojure.lang.IPersistentMap
  (-interceptor [t]
    (-> (merge interceptor-defaults t)
        (validate-interceptor)
        (map->Interceptor)))

  clojure.lang.Fn
  (-interceptor [t]
    (-interceptor {:name :handler
                   :enter (fn [ctx]
                            (assoc ctx :response (-> ctx :request t)))}))

  Interceptor
  (-interceptor [t]
    t)

  nil
  (-interceptor [t]
    nil))

(defn applies-to? [target]
  (fn [interceptor]
    (or (-> interceptor :system?)
        (-> interceptor :applies-to? (apply [target])))))

(defn- map-by-name [interceptors]
  (reduce (fn [acc {:as interceptor :keys [name]}]
            (when (contains? acc name)
              (throw (ex-info (str "multiple interceptors with name: " name) {})))
            (assoc acc name interceptor))
          {}
          interceptors))

(defn- post-order
  "Put `interceptors` in post-order.
  Can also be described as a reverse topological sort."
  [interceptors]
  (let [interceptors-by-name (map-by-name interceptors)]
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

(defn- sort-by-depends [interceptors]
  (concat (->> interceptors (filter :system?) post-order)
          (->> interceptors (remove :system?) post-order)))

(defn- append [interceptor interceptors]
  (concat interceptors [interceptor]))

;;
;; Public API:
;;

(defn into-interceptors
  ([interceptors]
   (into-interceptors (butlast interceptors) (last interceptors)))
  ([interceptors target]
   (into-interceptors interceptors target target))
  ([interceptors target handler]
   (->> interceptors
        (map (partial merge interceptor-defaults))
        (filter (applies-to? target))
        (map-by-name)
        (sort-by-depends)
        (append handler)
        (keep -interceptor))))

