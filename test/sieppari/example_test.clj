(ns sieppari.example-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [ring.util.http-response :as resp]
            [sieppari.core :as c]
            [sieppari.execute :as e]))

(defn handler
  "Sample handler"
  {:depends #{:session}
   :session? true}
  [request]
  (let [{:keys [username email]} (-> request :user)
        {:keys [message]} (-> request :body)]
    (resp/ok {:email email
              :message (str message ", " username)})))

(def db {:users {"1" {:id "1", :username "John", :email "john@mit.edu"}
                 "2" {:id "2", :username "Alan", :email "alan@bletchley.co.uk"}}})

(def db-interceptor
  {:name :db
   :enter (fn [ctx]
            (assoc ctx :db db))})

(def session-interceptor
  {:name :session
   :depends #{:db :http-resp}
   :applies-to? (fn [target] (-> target :session?))
   :enter (fn [ctx]
            (let [user-id (-> ctx
                              :request
                              :headers
                              (get "x-apikey"))
                  user (-> ctx
                           :db
                           :users
                           (get user-id))]
              (cond
                (nil? user-id) (assoc ctx :response (resp/unauthorized))
                (nil? user) (assoc ctx :response (resp/not-found))
                :else (update ctx :request assoc :user user))
              ;(when-not user-id (resp/unauthorized!))
              ;(when-not user (resp/not-found!))
              ;(update ctx :request assoc :user user)
              ))})

(def http-resp-interceptor
  {:name :http-resp
   :error (fn [ctx]
            (-> ctx
                (assoc :response (-> ctx :exception ex-data :response))
                (dissoc :exception)))})

(def interceptor-chain (-> [db-interceptor
                            session-interceptor
                            http-resp-interceptor]
                           (c/into-interceptors (meta #'handler) handler)))

(deftest execute-test
  (fact
    (->> {:body {:message "Hullo"}
          :headers {"x-apikey" "1"}}
         (e/execute interceptor-chain))
    => {:status 200
        :body {:email "john@mit.edu"
               :message "Hullo, John"}})
  (fact
    (->> {:body {:message "Hullo"}
          :headers {"x-apikey" "2"}}
         (e/execute interceptor-chain))
    => {:status 200
        :body {:email "alan@bletchley.co.uk"
               :message "Hullo, Alan"}})
  (fact
    (->> {:body {:message "Hullo"}
          :headers {"x-apikey" "3"}}
         (e/execute interceptor-chain))
    => {:status 404})
  (fact
    (->> {:body {:message "Hullo"}
          :headers {}}
         (e/execute interceptor-chain))
    => {:status 401}))
