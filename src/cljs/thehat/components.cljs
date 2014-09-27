(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentk]]
            [thehat.components.game-init :refer [game-init]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [clojure.string :refer [join]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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

(defn pause [{:keys [time]}]
  (dom/div
   (dom/div (str "PAUSE: " time))
   (dom/div "Team 2 prepare!")))

(defn in-progress [owner {:keys [team-1 team-2 words current-round time]
                          :as s}]
  (dom/div
   (dom/div
    (if (= current-round :team-1)
      "TEAM 1 turn"
      "TEAM 2 turn"))
   (dom/div time)
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
                      current-round (inc (get s current-round))
                      :words (into [] (drop 1 words)))))}
     "+")

    (dom/button
     {:on-click (fn []
                  (om/update-state!
                   owner #(assoc %
                            current-round (max 0 (dec (get s current-round)))
                            :words (into [] (drop 1 words)))))}
     "-"))))

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
                      :team-1 (inc team-2)
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
         :round-seq (rest round-seq)
         :interval (js/setInterval
                    (fn []
                      (let [{:keys [time current-round round-seq]} (om/get-state owner)]
                        (cond
                         (> time 1) (om/update-state! owner :time dec)
                         (= current-round :finish) (clear-interval owner)
                         :else (do
                                 (clear-interval owner)
                                 (interval owner)))))
                    1000))))))


(defcomponentk game-process [[:data deck-id decks game-ch :as data] owner]
  (init-state [_]
    {:interval nil
     :team-1 0
     :team-2 0
     :round-seq [{:name :team-1
                  :time 3}
                 {:name :pause
                  :time 5}
                 {:name :team-2
                  :time 3}
                 {:name :finish}]
     :current-round nil
     :words (into [] (get-words deck-id decks))})
  (will-mount [_]
    (interval owner))
  (render-state [_ {:keys [words time current-round interval]
                    :as s}]
    (dom/div
     (dom/h2 {:on-click (to-game-init game-ch)} "back")
     (cond
      (= current-round :pause) (pause s)
      (= (count words) 0) (do
                            (clear-interval owner)
                            (final-score owner s))

      (and (> time 0) (> (count words) 0)) (in-progress owner s)
      (> (count words) 0) (last-word owner s)
      :else (final-score owner s)))))

(def components {:game-init game-init
                 :game-process game-process
                 })

(defcomponent game [data owner]
  (init-state [_]
    {:ch (chan)
     :component :game-init
     :args {}})
  (will-mount [_]
    (let [{:keys [ch]} (om/get-state owner)]
      (go-loop []
        (let [c (<! ch)]
          (om/update-state! owner #(merge % c))
          (recur)))))
  (render-state [_ {:keys [component args ch]}]
    (let [c (component components)] 
      (dom/div
        (when c
          (om/build c (merge data args {:game-ch ch})))))))

(defcomponent rules [data owner]
  (render [_]
    (dom/div
     "Game rules")))

(defcomponent not-found [data owner]
  (render [_]
    (dom/div
     "Not found")))
