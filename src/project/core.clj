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
            [ring.middleware.session :refer [wrap-session]]))

(defn reg-errors [{:keys [pass-confirm] :as params}]
  (first
    (b/validate
      params
      :user [[v/required :message "name cannot be empty"]]
      :password [[v/required :message "password cannot be empty"]
                 [= pass-confirm :message "two passwords should same"]])))

(defn home-handle [request]
  (parser/render-file "index.html" request))
(defn game-handle [request]
  (parser/render-file "game.html" request))
(defn login-page [request]
  (parser/render-file "login.html" {}))
(defn register-page [request]
  (parser/render-file "register.html" {}))
(defn handle-login [user password request]
  (if (hashers/check password (:password (first (db/check-password user))))
    (game-handle (assoc-in request [:session :identity] user))
    (login-page (assoc request :error "user or password wrong"))))
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
        (-> {:result :ok}
            (resp/ok))
        (login-page req)))
    (catch Exception e
      (do
        (log/error e)))))

(defn handle-score [user level score req]
  (try
    (db/insert-score! user level score)
    (-> {:result :ok}
        (resp/ok))
    (parser/render "Hello {{name}}!" {:name "Yogthos"})
    (catch Exception e
      (do
        (log/error e)))))

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
