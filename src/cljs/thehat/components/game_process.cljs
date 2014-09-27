(ns thehat.components.game-process
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [thehat.helpers :as h :refer [nbsp]]
            [cljs.core.async :as async :refer [put!]]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn to-game-init [ch] #(put! ch {:component :game-init :args {}}))

(defn get-words [id decks]
  (some #(when (= id (:id %)) (:words %)) decks))

(defn next-team [t]
  (condp = t
    :team-1 :team-2
    :team-2 :team-1))

(defn final-score [owner {:keys [team-1 team-2]}]
  (dom/div
   (dom/b
    (case (compare team-1 team-2)
      1 "Team 1 won!!!"
      -1 "Team 2 won!!!"
      0 "DRAW"))
   (dom/h2 "Share result:")
   h/twitter
   h/facebook
   ))

(defn in-progress [owner name {:keys [team-1 team-2 words current-round
                                      time max-time max-words] :as s}]
  (dom/div
   {:class "game"}
   (dom/div
    {:class "time"}
    (dom/div
     {:class "progress active"
      :style {:width (str
                      (->> (/ time max-time)
                           (- 1)
                           (* 95)
                           (+ 5))
                      "%")}} time))

   (dom/div
    {:class "card-inner card-rotated"
     :id "rotated-card"
     :style {:background-image (.toDataUrl (.generate js/GeoPattern name))}}
    nbsp)

   (dom/div
    {:class "card-inner card-inner bounceInLeft animated" :id "current-card"}
    (dom/div
     {:class "word"}
     (first words)

     (dom/div
      {:class "buttons"}
      (dom/span {:class "icon-cancel-2 bt-wrong"
                 :on-click (fn []
                             (om/update-state!
                              owner #(assoc %
                                       current-round (max 0 (dec (get s current-round)))
                                       :words (into [] (drop 1 words)))))})
      (dom/span " ")
      (dom/span {:class "icon-checkmark bt-right"
                 :on-click (animate-card-out (sel1 :#current-card) owner current-round words)}))))

   (dom/div
    {:class "teams"}
    (dom/div
     {:class "team"}
     (dom/div {:class "arrow a1"} (dom/span {:class "icon-arrow-right"}))
     (dom/div {:class "team1"
              :style {:width (str
                              (->> (/ team-1 max-words)
                                   (* 90)
                                   (+ 5))
                              "%")}} team-1))
    (dom/div
     {:class "team"}
     (dom/div {:class "arrow a2"} (dom/span {:class "icon-arrow-right"}))
     (dom/div {:class "team2"
              :style {:width (str
                              (->> (/ team-2 max-words)
                                   (* 90)
                                   (+ 5))
                              "%")}} team-2)))))

(defn animate-card-out
  [div owner current-round words]
  (fn [] (do
           (dommy/remove-class! div "bounceInLeft")
           (dommy/add-class! div "bounceOutRight")
           (->> (fn [e] (do
                          (dommy/remove-class! div "bounceOutRight")
                          (dommy/add-class! div "bounceInLeft")
                          (om/update-state!
                           owner
                           #(assoc %
                              current-round (inc (get s current-round))
                              :words (into [] (drop 1 words))))))
                (dommy/listen! div :webkitAnimationEnd)))))

(defn last-word [owner {:keys [words team-1 team-2 current-team]}]
  (dom/div
   (dom/div "FINISHED")
   (dom/div (str "words count: " (count words)))
   (dom/div
    (dom/span
     (str "Teams 1: " team-1))
    (dom/span
     (str "Teams 2: " team-2)))

   (dom/div
    (dom/span
     (dom/b (first words)))

    (dom/button
     {:on-click (fn []
                  (om/update-state!
                   owner
                   #(assoc %
                      :team-1 (inc team-1)
                      :words [])))}
     "team-1")

    (dom/button
     {:on-click (fn []
                  (om/update-state!
                   owner
                   #(assoc %
                      :team-2 (inc team-2)
                      :words [])))}
     "team-2")

    (dom/button
     {:on-click (fn []
                  (om/update-state!
                   owner
                   #(assoc %
                      :words [])))}
     ":("))))

(defn clear-interval [owner]
  (-> (om/get-state owner)
      (:interval)
      (js/clearInterval))
  (om/set-state! owner :interval nil))

(defn interval [owner]
  (let [round-seq (-> (om/get-state owner)
                      (:round-seq))
        {:keys [name time]} (first round-seq)]
    (om/update-state!
     owner
     (fn [s]
       (assoc s
         :current-round name
         :time time
         :max-time time
         :round-seq (rest round-seq)
         :interval (js/setInterval
                    (fn []
                      (let [{:keys [time current-round round-seq]} (om/get-state owner)]
                        (cond
                         (> time 1) (om/update-state! owner :time dec)
                         (or
                          (= current-round :pause)
                          (= current-round :finish)) (clear-interval owner)
                         :else (do
                                 (clear-interval owner)
                                 (interval owner)))))
                    1000))))))

(defn pause [owner]
  (dom/div {:class "finished" :on-click #(interval owner)}
           (dom/div {:class "big"} (dom/span {:class "icon-flag"}))
           (dom/div "Round finished!")
           (dom/div {:class "small"}
                    "Switch team and then "
                    (dom/span {:class "mobile"} "tap")
                    (dom/span {:class "desktop"} "click")
                    " anywhere to start next round.")))

(defcomponentk game-process [[:data deck-id decks game-ch name :as data] owner]
  (init-state [_]
    {:interval nil
     :team-1 0
     :team-2 0
     :max-time 0
     :max-words 10
     :round-seq [{:name :team-1
                  :time 100}
                 {:name :pause}
                 {:name :team-2
                  :time 100}
                 {:name :finish}]
     :current-round nil
     :words (into [] (get-words deck-id decks))})
  (will-mount [_]
    (interval owner))
  (did-mount [_] (. js/window initFacebook))
  (render-state [_ {:keys [words time current-round interval]
                    :as s}]
    (cond
     ;; (= true true) (final-score owner s)
     (= current-round :pause) (pause owner)
     (= (count words) 0) (do
                           (clear-interval owner)
                           (final-score owner s))

     (and (> time 0) (> (count words) 0)) (in-progress owner name s)
     (> (count words) 0) (last-word owner s)
     :else (final-score owner s))))
