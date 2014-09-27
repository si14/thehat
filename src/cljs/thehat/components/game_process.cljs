(ns thehat.components.game-process
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [cljs.core.async :as async :refer [put!]]))

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
      0 "DRAW"))))

(defn in-progress [owner {:keys [team-1 team-2 words current-round time
                                 max-time max-words]
                          :as s}]
  (dom/div
   {:class "game"}
   (dom/div
    {:class "time"}
    (dom/div
     {:class "progress active"
      :style {:width (str
                      (->> (/ time max-time)
                           (- 1)
                           (* 100))
                      "%")}} time))

   (dom/div
    {:class "card-inner card-rotated"} "")

   (dom/div
    {:class "card-inner flipInX animated"}
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
      (dom/span {:class "icon-checkmark bt-right"
                 :on-click (fn []
                             (om/update-state!
                              owner
                              #(assoc %
                                 current-round (inc (get s current-round))
                                 :words (into [] (drop 1 words)))))}))))

   (dom/div
    {:class "teams"}
    (dom/div {:class "team1"
              :style {:width (str
                              (->> (/ team-1 max-words)
                                   (* 100))
                              "%")}} team-1)
    (dom/div {:class "team2"
              :style {:width (str
                              (->> (/ team-2 max-words)
                                   (* 100))
                              "%")}} team-2))))

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
  (dom/div
   (dom/div (str "PAUSE"))
   (dom/button
    {:on-click #(interval owner)}
    "Click to start team 2's turn!")))

(defcomponentk game-process [[:data deck-id decks game-ch :as data] owner]
  (init-state [_]
    {:interval nil
     :team-1 0
     :team-2 0
     :max-time 0
     :max-words 10
     :round-seq [{:name :team-1
                  :time 3}
                 {:name :pause}
                 {:name :team-2
                  :time 3}
                 {:name :finish}]
     :current-round nil
     :words (into [] (get-words deck-id decks))})
  (will-mount [_]
    (interval owner))
  (render-state [_ {:keys [words time current-round interval]
                    :as s}]
    (cond
     (= current-round :pause) (pause owner)
     (= (count words) 0) (do
                           (clear-interval owner)
                           (final-score owner s))

     (and (> time 0) (> (count words) 0)) (in-progress owner s)
     (> (count words) 0) (last-word owner s)
     :else (final-score owner s))))
