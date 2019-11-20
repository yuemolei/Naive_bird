(ns project.game
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [domina :as dom]
            [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
            [ajax.core :refer [GET POST]]
            [clojure.string :as str])
  (:require-macros
    [cljs.core.async.macros :refer [go-loop go]]))

(defn floor [x] (.floor js/Math x))
(defn translate [start-pos vel time]
  (floor (+ start-pos (* time vel))))
(def horiz-vel -0.15)
(def gravity (atom 0.05))
(def jump-vel 18)
(def start-y 312)
(def bottom-y 561)
(def flappy-x 212)
(def flappy-width 57)
(def flappy-height 41)
(def pillar-spacing 324)
(def pillar-gap (atom 158)) ;; 158
(def pillar-width 86)
(def current-score (atom 0))
(defn easy-init []
  (reset! gravity 0.05)
  (reset! pillar-gap 300))
(defn normal-init []
  (reset! gravity 0.05)
  (reset! pillar-gap 180))
(defn hard-init []
  (reset! gravity 0.08)
  (reset! pillar-gap 160))
(def starting-state { :timer-running false
                     :rank false
                     :jump-count 0
                     :initial-vel 0
                     :start-time 0
                     :flappy-start-time 0
                     :flappy-y   start-y
                     :button-y 363
                     :level 1
                     :pillar-list
                     [{ :start-time 0
                       :pos-x 900
                       :cur-x 900
                       :gap-top 200 }]})
(defn reset-state [_ cur-time level]
  (-> starting-state
      (update-in [:pillar-list] (fn [pls] (map #(assoc % :start-time cur-time) pls)))
      (assoc
        :start-time cur-time
        :flappy-start-time cur-time
        :timer-running true
        :rank false
        :level level)))
(defn set-rank-state [st cur-time]
  (-> st
      (assoc
        :rank true
        :button-y 500)))
(defonce flap-state (atom starting-state))
(defn curr-pillar-pos [cur-time {:keys [pos-x start-time] }]
  (translate pos-x horiz-vel (- cur-time start-time)))
(defn in-pillar? [{:keys [cur-x]}]
  (and (>= (+ flappy-x flappy-width)
           cur-x)
       (< flappy-x (+ cur-x pillar-width))))
(defn in-pillar-gap? [{:keys [flappy-y]} {:keys [gap-top]}]
  (and (< gap-top flappy-y)
       (> (+ gap-top @pillar-gap)
          (+ flappy-y flappy-height))))
(defn bottom-collision? [{:keys [flappy-y]}]
  (>= flappy-y (- bottom-y flappy-height)))
(declare get-rank)
(defn rank-list-component [response]
  (fn []
    [:div
     [:a#btn1.btn.btn-outline-success {:onClick #(get-rank 1)
                                    :style {:color "#FFF" :width "33%"}} "EASY"]
     [:a#btn2.btn.btn-outline-success {:onClick #(get-rank 2)
                                    :style {:color "#FFF" :width "33%"}} "NORMAL"]
     [:a#btn3.btn.btn-outline-success {:onClick #(get-rank 3)
                                    :style {:color "#FFF" :width "33%"}} "HARD"]
     [:div {:style {:display "grid"
                    :background-color "#aed0a6bd"
                    :border-radius "10px"
                    :box-shadow "7px 7px 5px 0px #00000036"}}
      [:ul [:div.rank-item "USER NAME"]
       [:div.rank-item "SCORE"]]
      [:ul [:div.rank-item (nth response 0)]
       [:div.rank-item (nth response 1)]]
      [:ul [:div.rank-item (nth response 2)]
       [:div.rank-item (nth response 3)]]
      [:ul [:div.rank-item (nth response 4)]
       [:div.rank-item (nth response 5)]]
      [:ul [:div.rank-item (nth response 6)]
       [:div.rank-item (nth response 7)]]
      [:ul [:div.rank-item (nth response 8)]
       [:div.rank-item (nth response 9)]]
      [:ul [:div.rank-item (nth response 10)]
       [:div.rank-item (nth response 11)]]
      [:ul [:div.rank-item (nth response 12)]
       [:div.rank-item (nth response 13)]]
      [:ul [:div.rank-item (nth response 14)]
       [:div.rank-item (nth response 15)]]
      [:ul [:div.rank-item (nth response 16)]
       [:div.rank-item (nth response 17)]]
      [:ul [:div.rank-item (nth response 18)]
       [:div.rank-item (nth response 19)]]]]))
(declare select-btn)
(defn handler-rank [response]
  (r/render
    (rank-list-component (str/split response #" "))
    (dom/by-id "rank-list"))
  (select-btn (nth (str/split response #" ") 20)))
(defn select-btn [level]
  (if (= level "1")
    (do
      (dom/add-class! (dom/by-id "btn1") "selected")
      (dom/remove-class! (dom/by-id "btn2") "selected")
      (dom/remove-class! (dom/by-id "btn3") "selected"))
    (if (= level "2")
      (do
        (dom/remove-class! (dom/by-id "btn1") "selected")
        (dom/add-class! (dom/by-id "btn2") "selected")
        (dom/remove-class! (dom/by-id "btn3") "selected"))
      (do
        (dom/remove-class! (dom/by-id "btn1") "selected")
        (dom/remove-class! (dom/by-id "btn2") "selected")
        (dom/add-class! (dom/by-id "btn3") "selected")))))
(defn get-rank [level]
  (GET "/get-rank" {:params {:level level}
                    :handler handler-rank}))
(defn post-score [st]
  (let [form-data (doto
                    (js/FormData.)
                    (.append "user" js/identity)
                    (.append "level" (:level st))
                    (.append "score" (:score st)))]
    (POST "/send-score" {:body form-data
                         :handler handler-rank})))
(defn endgame [s]
  (post-score s)
  (assoc s :timer-running false))
(defn collision? [{:keys [pillar-list] :as st}]
  (if (some #(or (and (in-pillar? %)
                      (not (in-pillar-gap? st %)))
                 (bottom-collision? st)) pillar-list)
    (endgame st)
    st))
(defn new-pillar [cur-time pos-x]
  {:start-time cur-time
   :pos-x      pos-x
   :cur-x      pos-x
   :gap-top    (+ 60 (rand-int (- bottom-y 120 @pillar-gap)))})
(defn update-pillars [{:keys [pillar-list cur-time] :as st}]
  (let [pillars-with-pos (map #(assoc % :cur-x (curr-pillar-pos cur-time %)) pillar-list)
        pillars-in-world (sort-by
                           :cur-x
                           (filter #(> (:cur-x %) (- pillar-width)) pillars-with-pos))]
    (assoc st
      :pillar-list
      (if (< (count pillars-in-world) 3)
        (conj pillars-in-world
              (new-pillar
                cur-time
                (+ pillar-spacing
                   (:cur-x (last pillars-in-world)))))
        pillars-in-world))))
(defn sine-wave [st]
  (assoc st
    :flappy-y
    (+ start-y (* 30 (.sin js/Math (/ (:time-delta st) 300))))))
(defn update-flappy [{:keys [time-delta initial-vel flappy-y jump-count] :as st}]
  (if (pos? jump-count)
    (let [cur-vel (- initial-vel (* time-delta @gravity))
          new-y   (- flappy-y cur-vel)
          new-y   (if (> new-y (- bottom-y flappy-height))
                    (- bottom-y flappy-height)
                    new-y)]
      (assoc st
        :flappy-y new-y))
    (sine-wave st)))
(defn score [{:keys [cur-time start-time] :as st}]
  (let [score (- (.abs js/Math (floor (/ (- (* (- cur-time start-time) horiz-vel) 544)
                                         pillar-spacing)))
                 4)]
    (assoc st :score (if (neg? score) 0 score))))
(defn time-update [timestamp state]
  (-> state
      (assoc
        :cur-time timestamp
        :time-delta (- timestamp (:flappy-start-time state)))
      update-flappy
      update-pillars
      collision?
      score))
(defn jump [{:keys [cur-time jump-count] :as state}]
  (-> state
      (assoc
        :jump-count (inc jump-count)
        :flappy-start-time cur-time
        :initial-vel jump-vel)))
(defn border [{:keys [cur-time] :as state}]
  (-> state
      (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

(defn pillar-offset [{:keys [gap-top] :as p}]
  (assoc p
    :upper-height gap-top
    :lower-height (- bottom-y gap-top @pillar-gap)))

(defn pillar-offsets [state]
  (update-in state [:pillar-list]
             (fn [pillar-list]
               (map pillar-offset pillar-list))))


(defn world [state]
  (-> state
      border
      pillar-offsets))

(defn px [n] (str n "px"))

(defn pillar [{:keys [cur-x pos-x upper-height lower-height]}]
  [:div.pillars
   [:div.pillar.pillar-upper {:style {:left (px cur-x)
                                      :height upper-height}}]
   [:div.pillar.pillar-lower {:style {:left (px cur-x)
                                      :height lower-height}}]])
(defn time-loop [time]
  (let [new-state (swap! flap-state (partial time-update time))]
    (when (:timer-running new-state)
      (go
        (<! (timeout 16))
        (if-not (compare-and-set! current-score (:score new-state) (:score new-state))
          (do
            (post-score new-state)
            (reset! current-score (:score new-state))))
        (.requestAnimationFrame js/window time-loop)))))

(defn score-loop [time]
  (if (:timer-running flap-state)
    (let [new-state (swap! flap-state (partial time-update time))]
      (when (:timer-running new-state)
        (go
          (post-score new-state)
          (<! (timeout 2000))
          (.requestAnimationFrame js/window score-loop))))))

(defn start-game [level]
  (reset! current-score 0)
  (dom/add-class! (dom/by-id "rank-list") "disappear")
  (get-rank level)
  (if (= level 1)
    (easy-init)
    (if (= level 2)
      (normal-init)
      (hard-init)))
  (.requestAnimationFrame
    js/window
    (fn [time]
      (reset! flap-state (reset-state @flap-state time level))
      (time-loop time))))
(defn show-rank [level]
  (dom/remove-class! (dom/by-id "rank-list") "disappear")
  (.requestAnimationFrame
    js/window
    (fn [time]
      (reset! flap-state (set-rank-state @flap-state time)))))

(defn log-component? [name]
  (fn []
    [:div {:style {:display "contents"}}
     [:span.navbar-text (str "Welcome " name)]
     [:a.btn.btn-sm.btn-outline-secondary {:href "/logout"} "Sign out"]]))

(defn main-component [{:keys [score cur-time jump-count
                              timer-running rank border-pos
                              flappy-y button-y pillar-list
                              list level]}]
  (fn []
    [:div [:div.login-box.d-flex.justify-content-end.align-items-center {:style {:position "absolute"
                                                                                 :padding "0 10px"}}
           (if-not (= js/identity "")
             [log-component? js/identity]
             [:a.btn.btn-sm.btn-outline-secondary {:href "/login"} "Sign in"])]
     [:div.board { :onMouseDown (fn [e]
                                  (swap! flap-state jump)
                                  (.preventDefault e))}
      (if-not rank
        [:h1.score score])
      (if-not timer-running
        [:a.button.button-3d.button-action.button-pill {:onClick #(start-game 1)
                                                        :style {:top (px button-y)
                                                                :z-index "5"
                                                                :color "#FFF"
                                                                :margin "0 10px"}} "EASY"]
        [:span])
      (if-not timer-running
        [:a.button.button-3d.button-action.button-pill {:onClick #(start-game 2)
                                                        :style {:top (px button-y)
                                                                :z-index "5"
                                                                :color "#FFF"
                                                                :margin "0 10px"}} "NORMAL"]
        [:span])
      (if-not timer-running
        [:a.button.button-3d.button-action.button-pill {:onClick #(start-game 3)
                                                        :style {:top (px button-y)
                                                                :z-index "5"
                                                                :color "#FFF"
                                                                :margin "0 10px"}} "HARD"]
        [:span])
      [:div (map pillar pillar-list)]
      [:div.flappy {:style {:top (px flappy-y)}}]
      [:div.scrolling-border {:style { :background-position-x (px border-pos)}}]
      [:div.namebox
       (if-not timer-running
         (if (and (< 0 score)
                  (= false rank))
           [:a.button.button-3d.button-action.button-pill {:onClick #(show-rank level)
                                                           :style {:z-index "5"
                                                                   :color "#FFF"}} "Rank"]
           [:span])
         [:span])]]
     [:div#rank-list.rank.disappear]]))

(defn ^:export init []
  (let [node (dom/by-id "board-area")]
      (defn renderer [full-state]
        (r/render (main-component full-state) node)))
  (add-watch flap-state :renderer (fn [_ _ _ n]
                                    (renderer (world n))))
  (reset! flap-state @flap-state)
  (get-rank 1))
