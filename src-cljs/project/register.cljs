(ns project.register
  (:require [domina :as dom]
            [domina.events :as ev]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ajax]
            [reagent.session :as session]
            [taoensso.timbre :as log]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn validate-user [user t]
  (if (> (count user) 0)
    true
    false))
(defn validate-password [password t]
  (if (> (count password) 0)
    true
    false))
(defn validate-pass-confirm [password t]
  (if (= password t)
    true
    false))
(defn validate-invalid [input vali-fun input2]
  (if-not (vali-fun (dom/value input) (dom/value input2))
    (dom/add-class! input "is-invalid")
    (dom/remove-class! input "is-invalid"))
  (if (= (dom/value input) (dom/value input2))
    (dom/remove-class! input2 "is-invalid")))

(defn validate-form []
  (let [user (dom/by-id "user")
        password (dom/by-id "password")
        pass-confirm (dom/by-id "pass-confirm")]
    (if-not (validate-user (dom/value user) "0")
      (do
        (js/alert "name cannot be empty")
        false)
      (if-not (validate-password (dom/value password) "0")
        (do
          (js/alert "password cannot be empty")
          false)
        (if-not (= (dom/value password) (dom/value pass-confirm))
          (do
            (js/alert "two passwords should same")
            false)
          true)))))

(defn register-component []
  (fn []
    [:div.container {:id "board-area"}
     [:form#loginForm.form-signin
      {:action "/register" :method "post"}
      (if (= js/error "user already exist")
        [:h1.h3.mb-3.font-weight-normal.text-center "user already exist"]
        [:h1.h3.mb-3.font-weight-normal.text-center "Register"])
      [:div
       [:label.sr-only "user" "user"]
       [:input#user.form-control
        {:type "text" :name "user" :autoComplete "off" :auto-focus true :placeholder "Name"
         :on-blur #(validate-invalid (dom/by-id "user") validate-user "0")}]
       [:div.invalid-feedback {:style {:position "absolute"
                                       :width "250px"}} "invalid name"]]
      [:div.button-space
       [:label.sr-only "password" "password"]
       [:input#password.form-control
        {:type "password" :name "password" :placeholder "Password"
         :on-blur #(validate-invalid (dom/by-id "password") validate-password (dom/by-id "pass-confirm"))}]
       [:div.invalid-feedback {:style {:position "absolute"
                                       :width "250px"}} "invalid password"]]
      [:div.button-space
       [:label.sr-only "pass-confirm" "pass-confirm"]
       [:input#pass-confirm.form-control
        {:type "password" :name "pass-confirm" :placeholder "Password confirm"
         :on-blur #(validate-invalid (dom/by-id "pass-confirm") validate-pass-confirm (dom/by-id "password"))}]
       [:div.invalid-feedback {:style {:position "absolute"
                                       :width "250px"}} "password not same"]]
      [:div#error]
      [:div.button-space
       [:input#submit.button.button-3d.button-action.button-pill
        {:type "submit" :value "Submit"}]]
      [:div.button-space
       [:a {:href "/login"} "Go back"]]]]))

(defn ^:export init []
    (reagent/render
      [register-component]
      (dom/by-id "board-area"))
    (let [login-form (dom/by-id "loginForm")]
      (set! (.-onsubmit login-form) validate-form)))
