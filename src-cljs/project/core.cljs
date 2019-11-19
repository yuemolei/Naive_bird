(ns project.core
  (:require [project.login :as login]
            [project.register :as register]
            [project.game :as game]
            [reagent.core :as r]
            [reagent.session :as session]
            [domina :as dom]))

(defn log-component? [name]
  (fn []
    [:div {:style {:display "contents"}}
     [:span.navbar-text (str "Welcome " name)]
     [:a.btn.btn-sm.btn-outline-secondary {:href "/logout"} "Sign out"]]))

(defn main-component []
      (fn []
        [:div
         [:div.login-box.d-flex.justify-content-end.align-items-center
          (if-not (= js/identity "")
            [log-component? js/identity]
            [:a.btn.btn-sm.btn-outline-secondary {:href "/login"} "Sign in"])]
         [:div.title
               "Welcome to Naive Frog !!!"]
         [:div.flappy]]))

(defn home-component []
      [:div.container
       [main-component]])

(defn ^:export init []
      (if (and js/document
               (.-getElementById js/document))
        (r/render
          [home-component]
          (dom/by-id "board-area"))))