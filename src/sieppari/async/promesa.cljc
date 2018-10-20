(ns sieppari.async.promesa
  (:require [sieppari.async :as sa]
            [promesa.core :as p]
            [clojure.string :as str])
  #?(:clj (:import (java.util.concurrent CompletionStage))))

#?(:clj
   (extend-protocol sa/AsyncContext
     CompletionStage
     (continue [this f] (p/chain this f))
     (await [this] (deref this))))

#?(:cljs
   (extend-protocol sa/AsyncContext
     p/Promise
     (continue [this f] (p/chain this f))

     ;; We need to extend js/Promise again here because it seems like promesa
     ;; does some magic with the object definition:
     ;; https://github.com/funcool/promesa/blob/master/src/promesa/impl.cljc#L47
     ;;
     ;; Without the protocol extension on js/Promise is lost. See also:
     ;; https://github.com/funcool/promesa/issues/58
     js/Promise
     (continue [t f] (.then t f))))
