(ns project.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [compojure.route :as route]
            [compojure.core :refer [routes GET defroutes POST]]
            [selmer.parser :as parser]
            [ring.util.response :refer [redirect]]
            [project.models.db :as db]
            [ring.util.http-response :as resp]
            [buddy.hashers :as hashers]
            [taoensso.timbre :as log]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [ring.middleware.session :refer [wrap-session]]
            [cognitect.transit :as transit]
            [clojure.string :as str]))

(defn home-handle [request]
  (parser/render-file "index.html" request))
(defn game-handle [request]
  (parser/render-file "game.html" request))
(defn login-page [request]
  (parser/render-file "login.html" request))
(defn register-page [request]
  (parser/render-file "register.html" request))
(defn handle-login [user password request]
  (try
    (if (hashers/check password (:password (first (db/check-password user))))
      (game-handle (assoc-in request [:session :identity] user))
      (login-page (assoc request :error "user or password wrong")))
    (catch Exception e
      (do
        (login-page (assoc request :error "user or password wrong"))))))
(defn handle-logout [request]
  (do
    (assoc request :session {})
    (redirect "/")))
(defn error-page [error-details]
  {:status (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (parser/render-file "error.html" error-details)})

(defn register! [req user password confirm]
  (try
    (if-not (= nil (first (db/select-user user)))
      (register-page (assoc req :error "user already exist"))
      (do
        (db/save-user! user (hashers/derive password))
        (db/init-score! user)
        (-> {:result :ok}
            (resp/ok))
        (login-page req)))
    (catch Exception e
      (do
        (log/error e)))))
(defn return-rank [level]
  (let [rank-list (db/rank level)]
    (let [user0 (:user (nth rank-list 0))
          score0 (:score (nth rank-list 0))
          user1 (:user (nth rank-list 1))
          score1 (:score (nth rank-list 1))
          user2 (:user (nth rank-list 2))
          score2 (:score (nth rank-list 2))
          user3 (:user (nth rank-list 3))
          score3 (:score (nth rank-list 3))
          user4 (:user (nth rank-list 4))
          score4 (:score (nth rank-list 4))
          user5 (:user (nth rank-list 5))
          score5 (:score (nth rank-list 5))
          user6 (:user (nth rank-list 6))
          score6 (:score (nth rank-list 6))
          user7 (:user (nth rank-list 7))
          score7 (:score (nth rank-list 7))
          user8 (:user (nth rank-list 8))
          score8 (:score (nth rank-list 8))
          user9 (:user (nth rank-list 9))
          score9 (:score (nth rank-list 9))]
      (parser/render "{{user0}} {{score0}} {{user1}} {{score1}} {{user2}} {{score2}} {{user3}} {{score3}} {{user4}} {{score4}} {{user5}} {{score5}} {{user6}} {{score6}} {{user7}} {{score7}} {{user8}} {{score8}} {{user9}} {{score9}} {{level}}"
                     {:user0 user0 :score0 score0
                      :user1 user1 :score1 score1
                      :user2 user2 :score2 score2
                      :user3 user3 :score3 score3
                      :user4 user4 :score4 score4
                      :user5 user5 :score5 score5
                      :user6 user6 :score6 score6
                      :user7 user7 :score7 score7
                      :user8 user8 :score8 score8
                      :user9 user9 :score9 score9 :level level}))))
(defn handle-score [user level score req]
  (try
    (if (< (:score (first (db/get-score user level))) (Integer. score))
      (db/update-score! user level score))
    (-> {:result :ok}
        (resp/ok))
    (return-rank level)
    (catch Exception e
      (do
        (parser/render "Sorry {{name}}!" {:name "Yogthos"})))))

(defn handle-rank [level req]
  (try
    (-> {:result :ok}
        (resp/ok))
    (return-rank level)
    (catch Exception e
      (do
        (parser/render "Sorry {{name}}!" {:name "Yogthos"})))))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"]
                  "no-cache"))))
(def app-routes
  (routes
    (GET "/" request (home-handle request))
    (GET "/login" request (login-page request))
    (POST "/login" [user password :as req] (handle-login user password req))
    ;(GET "/game" request (game-handle request))
    (GET "/logout" request (handle-logout request))
    (GET "/about" [] (str "This is about page."))
    (GET "/register" req (register-page req))
    (POST "/register" [user password :as req]
      (register! req user password (:user req)))
    (POST "/send-score" [user level score :as req] (handle-score user level score req))
    (GET "/get-rank" [level :as req] (handle-rank level req))
    (route/not-found error-page)))
(def app
  (-> app-routes
      (wrap-nocache)
      (wrap-reload)
      (wrap-webjars)
      (wrap-session)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
(defn -main []
  (jetty/run-jetty
    app
    {:port 3000
     :join? false}))
