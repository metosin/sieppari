(ns sieppari.core
  (:require [alandipert.kahn :as kahn]))

(defrecord Interceptor [name
                        enter
                        leave
                        error
                        applies?
                        depends])

(def interceptor-defaults {:enter identity
                           :leave identity
                           :error identity
                           :applies-to? (constantly true)
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

(defn- error-if-circular! [sorted]
  (when-not (seq sorted)
    (throw (ex-info "interceptors have circular dependency" {})))
  sorted)

(defn topology-sort [interceptors]
  (->> interceptors
       (map (fn [{:keys [name depends]}]
              [name depends]))
       (into {})
       (kahn/kahn-sort)
       (error-if-circular!)
       (reverse)))

(defn- sort-by-depends [interceptors-map]
  (->> interceptors-map
       (vals)
       (topology-sort)
       (map interceptors-map)))

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

