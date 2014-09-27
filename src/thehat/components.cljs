(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [clojure.string :refer [join]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(declare game-init)
(defn to-game-init [ch] #(put! ch {:component game-init :args {}}))

(defn get-words [id decks]
  (some #(when (= id (:id %)) (:words %)) decks))

(defn next-team [t]
  (condp = t
    :team-1 :team-2
    :team-2 :team-1))

(defcomponent game-process [{:keys [deck-id decks game-ch]
                             :as data} owner]
  (init-state [_]
    {:interval nil
     :time 3
     :team-1 0
     :team-2 0
     :current-team :team-1
     :words (into [] (get-words deck-id decks))})
  (will-mount [_]
    (om/set-state!
     owner
     :interval
     (js/setInterval
      #(let [{:keys [time]} (om/get-state owner)]
         (if (> time 0)
           (om/update-state! owner :time dec)
           (-> (om/get-state owner)
               (:interval)
               (js/clearInterval))))
      1000)))
  (render-state [_ {:keys [time words current-team team-1 team-2]
                    :as s}]
    (dom/div
     (dom/h2 {:on-click (to-game-init game-ch)} "back")
     (if (> time 0)
       (dom/div time)
       (dom/div "FINISHED"))

     (dom/div {:class "t"} (str "words count: " (count words)))

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
                        current-team (inc (get s current-team))
                        :current-team (next-team current-team)
                        :words (into [] (drop 1 words)))))}
       "+")

      (dom/button
       {:on-click (fn []
                    (om/update-state!
                     owner #(assoc %
                              :current-team (next-team current-team)
                              :words (into [] (drop 1 words)))))}
       "-")))))

(defn select-deck
  [id ch]
  #(put! ch {:component game-process :args {:deck-id id}}))

(defn deck [{:keys [name id]} game-ch]
  (dom/h1 {:class "deck" :on-click (select-deck id game-ch)} name))

(defcomponent game-init [{:keys [game-ch decks] :as data} owner]
  (render [_]
    (dom/div
      (dom/h3 (str "Select one of " (count decks) " decks:"))
      (map #(deck % game-ch) decks))))

(defcomponent game [data owner]
  (init-state [_]
    {:ch (chan)
     :component game-init
     :args {}})
  (will-mount [_]
    (let [{:keys [ch]} (om/get-state owner)]
      (go-loop []
        (let [{:keys [component args]
               :as c} (<! ch)]
          (om/update-state! owner #(merge % c))
          (recur)))))
  (render-state [_ {:keys [component args ch]}]
    (dom/div
     (when component
       (om/build component (merge data args {:game-ch ch}))))))

(defcomponent rules [data owner]
  (render [_]
    (dom/div
     "Game rules")))

(defcomponent not-found [data owner]
  (render [_]
    (dom/div
     "Not found")))
