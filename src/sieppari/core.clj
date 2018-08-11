(ns sieppari.core
  (:require [alandipert.kahn :as kahn]
            [clojure.string :as str]))

(defrecord Interceptor [name
                        enter
                        leave
                        error
                        applies?
                        depends])

(defn interceptor? [value]
  (instance? Interceptor value))

(def interceptor-defaults {:enter identity
                           :leave identity
                           :error identity
                           :applies-to? (constantly true)
                           :depends #{}})

(defn validate-interceptor [interceptor]
  (when-not (-> interceptor (contains? :name))
    (throw (ex-info "interceptor :name is mandatory" {:interceptor interceptor})))
  (when-not (-> interceptor :name keyword?)
    (throw (ex-info "interceptor :name must be a keyword" {:interceptor interceptor})))
  (when-let [non-fns (->> (map (juxt identity interceptor)
                               [:enter :leave :error :applies-to?])
                          (remove (comp fn? second))
                          (map first)
                          (seq))]
    (println (pr-str non-fns))
    (throw (ex-info (format "interceptor %s must be a function"
                            (str/join non-fns ","))
                    {:interceptor interceptor})))
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

(defn- index-by-name [interceptors]
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

(defn- topology-sort [interceptors]
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
  ([interceptors handler target]
   (->> interceptors
        (remove nil?)
        (map (partial merge interceptor-defaults))
        (filter #(-> % :applies-to? (apply [target])))
        (index-by-name)
        (sort-by-depends)
        (append handler)
        (map -interceptor)))
  ([interceptors handler]
   (into-interceptors interceptors handler handler))
  ([interceptors]
   (into-interceptors (butlast interceptors) (last interceptors))))
