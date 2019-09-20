(ns sieppari.util)

(defn exception? [e]
  (instance? #?(:clj Exception :cljs js/Error) e))
