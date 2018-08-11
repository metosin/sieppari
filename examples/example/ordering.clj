(ns example.ordering
  (:require [sieppari.core :as sc]
            [sieppari.execute.sync :as ses]
            [sieppari.ordering :as ordering]
            [sieppari.sieppari.util.graphviz :as sg]))

(defn make-interceptor [name depends]
  {:name name
   :depends depends
   :enter (fn [ctx] (println "ENTER" name) ctx)
   :leave (fn [ctx] (println "LEAVE" name) ctx)})

(def interceptors [(make-interceptor :a nil)
                   (make-interceptor :b #{:a})
                   (make-interceptor :c #{:b})
                   (make-interceptor :d #{:c :a})
                   (make-interceptor :e #{:c :b})
                   (make-interceptor :f #{:b :e})])

(defn handler [request]
  (println "HANDLER:" (pr-str request))
  "world!")

(def chain (-> interceptors
               (ordering/dependency-order)
               (ordering/append handler)
               (sc/into-interceptors)))

(ses/execute chain "Hello")
; Prints:
;  ENTER :a
;  ENTER :b
;  ENTER :c
;  ENTER :d
;  ENTER :e
;  ENTER :f
;  HANDLER: "Hello"
;  LEAVE :f
;  LEAVE :e
;  LEAVE :d
;  LEAVE :c
;  LEAVE :b
;  LEAVE :a
;=> "world!"

(comment

  (-> interceptors
      (sg/interceptors-dependency->graph)
      (sg/multidigraph)
      (sg/viz-graph
        {:layout :dot
         :save {:filename "docs/example.ordering.1.png" :format :png}
         }))

  (-> interceptors
      (ordering/dependency-order)
      (sg/interceptors-chain->graph)
      (sg/multidigraph)
      (sg/viz-graph
        {:layout :circo
         :save {:filename "docs/example.ordering.2.png" :format :png}
         }))

  (interceptors->graph interceptors))

(comment

  (ordering/dependency-order )

  (interceptors->graph interceptors)



  (-> (uber/multigraph
        [:Artemis :Balela]
        [:Artemis :Balela ]
        [:Artemis :Coulton]
        [:Artemis :Dentana]
        [:Balela :Coulton ]
        [:Balela :Egglesberg ])
      (uber/viz-graph {:save {:filename "./deps.png" :format :png}})))
