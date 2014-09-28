(ns thehat.components.game-init
  (:require [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [thehat.helpers :refer [nbsp]]
            [cljs.core.async :as async :refer [put!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn select-deck
  [id ch name]
  #(do
     (dommy/remove-class! (sel1 (str "#deck_" id)) "flipInX")
     (dommy/add-class! (sel1 (str "#deck_" id)) "bounce")
     (dommy/listen! (sel1 (str "#deck_" id))
                    :webkitAnimationEnd (fn [e] (put! ch {:component :game-process
                                                          :args {:deck-id id :name name}})))))

(defn deck [{:keys [name id words]} game-ch]
  (dom/div {:id (str "deck_" id)
            :class "pack animated flipInX"
            :on-click (select-deck id game-ch name)}
   (dom/div {:class "inside rotated"} "&nbsp;")
   (dom/div {:class "inside"
            :style {:background-image (.toDataUrl (.generate js/GeoPattern name))}}
    (dom/div {:class "word"} name
             (dom/div {:class "small"} "Number of words: " (count words))))))

(defcomponentk game-init [[:data game-ch decks :as data] owner]
  (render [_]
    (dom/div {:class "chooser"}
      (dom/div {:class "chooser-inner"} (dom/div {:class "title"} "Choose package:")
        (map #(deck % game-ch) decks)))))

