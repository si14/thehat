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
  (some #(when (= id (:id %) (:words %))) decks))

(defcomponent game-process [{:keys [deck-id game-ch]
                             :as data} owner]
  (init-state [_]
    {:time 10})
  (will-mount [_]
    (let [interval (js/setInterval
                    #(let [{:keys [time]} (om/get-state owner)]
                       (if (> time 0)
                         (om/update-state! owner :time dec)
                         (js/clearInterval interval)))
                    1000)]))
  (render-state [_ {:keys [time]}]
    (dom/div
     (dom/h2 {:on-click (to-game-init game-ch)} "back")
     (if (> time 0)
       (dom/span time)
       (dom/span "FINISHED"))
     (dom/span (str "words count: " (count words)))
     (dom/span (join ", " words)))))

(defn select-deck
  [words ch]
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
          (om/set-state! owner c)
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
