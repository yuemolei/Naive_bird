(ns project.login
  (:require [domina :as dom]
            [domina.events :as ev]
            [reagent.core :as reagent :refer [atom]]))

(defn validate-user [user]
  (if (> (count user) 0)
    true
    false))
(defn validate-passoword [password]
  (if (> (count password) 0)
    true
    false))
(defn validate-invalid [input-id vali-fun]
      (if-not (vali-fun (dom/value input-id))
              (dom/add-class! input-id "is-invalid")
              (dom/remove-class! input-id "is-invalid")))

(defn validate-form []
      (let [user (dom/by-id "user")
            password (dom/by-id "password")]
           (if (and (validate-user (dom/value user))
                    (validate-passoword (dom/value password)))
             true
             (do
               (js/alert "name or password cannot be empty")
               false))))

(defn login-component []
      [:div {:id "board-area"}
       [:form#loginForm.form-signin
        {:action "/login" :method "post"}
        (if (= js/error "user or password wrong")
          [:h1.h3.mb-3.font-weight-normal.text-center "name or password wrong"]
          [:h1.h3.mb-3.font-weight-normal.text-center "Please sign in"])
        [:div
         [:label.sr-only "user" "user"]
         [:input#user.form-control
          {:type "text" :name "user" :autoComplete "off" :auto-focus true :placeholder "Name"
           :on-blur #(validate-invalid (dom/by-id "user") validate-user)}]
         [:div.invalid-feedback {:style {:position "absolute"
                                         :width "250px"}} "invalid name"]]
        [:div.button-space
         [:label.sr-only "password" "password"]
         [:input#password.form-control
          {:type "password" :name "password" :placeholder "Password"
           :on-blur #(validate-invalid (dom/by-id "password") validate-passoword)}]
         [:div.invalid-feedback {:style {:position "absolute"
                                         :width "250px"}} "invalid password"]]
        [:div#error]
        [:div.button-space
         [:input#submit.button.button-3d.button-action.button-pill
          {:type "submit" :value "Sign In"}]]
        [:div.button-space
         [:a {:href "/register"} "Sign up now!"]]]])

(defn ^:export init []
      (if (and js/document
               (.-getElementById js/document))
        (reagent/render
          [login-component]
          (dom/by-id "board-area")))
      (if (and js/document
               (.-getElementById js/document))
        (let [login-form (dom/by-id "loginForm")]
             (set! (.-onsubmit login-form) validate-form))))
