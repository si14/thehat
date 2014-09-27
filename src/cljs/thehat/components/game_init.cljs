(ns thehat.components.game-init
  (:require [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [cljs.core.async :as async :refer [put!]]))

(defn select-deck
  [id ch]
  #(put! ch {:component :game-process :args {:deck-id id}}))

(defn deck [{:keys [name id]} game-ch]
  (dom/div {:class "pack" :on-click (select-deck id game-ch)}
   (dom/div {:class "inside rotated"} "&nbsp;")
   (dom/div {:class "inside"}
    (dom/div {:class "word"} name))))

(defcomponentk game-init [[:data game-ch decks :as data] owner]
  (render [_]
    (dom/div {:class "chooser"}
      (dom/div {:class "chooser-inner"} (dom/div {:class "title"} "Choose package:")
        (map #(deck % game-ch) decks)))))

