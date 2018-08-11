(ns sieppari.filtering)

(defn applies-to [target]
  (fn [interceptor]
    (-> interceptor
        :applies-to?
        (or (constantly true))
        (apply [target]))))
