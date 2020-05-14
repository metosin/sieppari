(ns sieppari.core-async-test
  (:require [clojure.test :refer [deftest is async]]
            [clojure.core.async :refer [go]]
            [sieppari.core :as sc]
            [sieppari.async.core-async]))

(defn make-logging-interceptor [log name]
  {:name name
   :enter (fn [ctx] (swap! log conj [:enter name]) ctx)
   :leave (fn [ctx] (swap! log conj [:leave name]) ctx)
   :error (fn [ctx] (swap! log conj [:error name]) ctx)})


(defn make-async-logging-interceptor [log name]
  {:name name
   :enter (fn [ctx] (swap! log conj [:enter name]) (go ctx))
   :leave (fn [ctx] (swap! log conj [:leave name]) (go ctx))
   :error (fn [ctx] (swap! log conj [:error name]) (go ctx))})

(defn make-logging-handler [log]
  (fn [request]
    (swap! log conj [:handler])
    request))

(defn make-async-logging-handler [log]
  (fn [request]
    (swap! log conj [:handler])
    (go request)))

(def request {:foo "bar"})
(def error (ex-info "oh no" {}))

(defn fail! [& _]
  (throw (ex-info "Should never be called" {})))

(deftest chan-happy-sync-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (make-logging-handler log)]
          (sc/execute request
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:leave :c]
                                [:leave :b]
                                [:leave :a]]))
                        (is (= response request))
                        (done))
                      fail!)))))

(deftest chan-happy-async-test
  (async done
    (let [log (atom [])]
      (-> [(make-async-logging-interceptor log :a)
           (make-async-logging-interceptor log :b)
           (make-async-logging-interceptor log :c)
           (make-logging-handler log)]
          (sc/execute request
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:leave :c]
                                [:leave :b]
                                [:leave :a]]))
                        (is (= response request))
                        (done))
                      fail!)))))

(deftest chan-async-b-sync-execute-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-async-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (make-logging-handler log)]
          (sc/execute request
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:leave :c]
                                [:leave :b]
                                [:leave :a]]))
                        (is (= response request))
                        (done))
                      fail!)))))

(deftest chan-async-handler-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (make-async-logging-handler log)]
          (sc/execute request
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:leave :c]
                                [:leave :b]
                                [:leave :a]]))
                        (is (= response request))
                        (done))
                      fail!)))))

(deftest chan-async-execute-with-handler-throws-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (fn [_]
             (swap! log conj [:handler])
             (throw error))]
          (sc/execute request
                      fail!
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:error :c]
                                [:error :b]
                                [:error :a]]))
                        (is (= response error))
                        (done)))))))

(deftest chan-async-execute-rejection-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (fn [_]
             (swap! log conj [:handler])
             (go error))]
          (sc/execute request
                      fail!
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:error :c]
                                [:error :b]
                                [:error :a]]))
                        (is (= response error))
                        (done)))))))

(deftest chan-async-failing-handler-test
  (async done
    (let [log (atom [])]
      (-> [(make-logging-interceptor log :a)
           (make-logging-interceptor log :b)
           (make-logging-interceptor log :c)
           (fn [_]
             (swap! log conj [:handler])
             (go error))]
          (sc/execute request
                      fail!
                      (fn [response]
                        (is (= @log
                               [[:enter :a]
                                [:enter :b]
                                [:enter :c]
                                [:handler]
                                [:error :c]
                                [:error :b]
                                [:error :a]]))
                        (is (= response error))
                        (done)))))))

(deftest chan-async-failing-handler-b-fixes-test
  (let [make-fixing-error-b (fn [log]
                              (-> (make-async-logging-interceptor log :b)
                                  (assoc :error (fn [ctx]
                                                  (swap! log conj [:error :b])
                                                  (go
                                                    (-> ctx
                                                        (assoc :error nil)
                                                        (assoc :response :fixed-by-b)))))))]
    (async done
      (let [log (atom [])]
        (-> [(make-logging-interceptor log :a)
             (make-fixing-error-b log)
             (make-logging-interceptor log :c)
             (fn [_]
               (swap! log conj [:handler])
               (go error))]

            (sc/execute request
                        (fn [response]
                          (is (= @log
                                 [[:enter :a]
                                  [:enter :b]
                                  [:enter :c]
                                  [:handler]
                                  [:error :c]
                                  [:error :b]
                                  [:leave :a]]))
                          (is (= response :fixed-by-b))
                          (done))
                        fail!))))))
