(ns example.compile
  (:require [sieppari.core :as sc]
            [sieppari.execute :as se]
            [sieppari.compile :as scc]))

; Make an interceptor with given name, interceptor records
; invocations to ctx for later analysis:

(defn make-interceptor [name]
  {:enter (fn [ctx] (println "ENTER:" name) ctx)
   :leave (fn [ctx] (println "LEAVE:" name) ctx)
   :error (fn [ctx] (println "ERROR:" name) ctx)})

; Test stack with three interceptors and a handler that response
; with `(inc request)`:

(def interceptor-chain (-> [(make-interceptor :a)
                            (make-interceptor :b)
                            (make-interceptor :c)
                            inc]
                           (sc/into-interceptors)))

(se/execute interceptor-chain 41)
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  LEAVE: :c
;  LEAVE: :b
;  LEAVE: :a
;=> 42

(def compiled-chain (scc/compile-interceptor-chain interceptor-chain))

(compiled-chain 41)
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  LEAVE: :c
;  LEAVE: :b
;  LEAVE: :a
;=> 42
