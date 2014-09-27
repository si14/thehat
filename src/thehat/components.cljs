(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defcomponent deck [data owner]
  (render [_]
    (dom/div {:class "deck" :data-deck-id (:id data)} (:name data))))

(defcomponent game [data owner]
  (render [_]
    (dom/div
      (dom/h3 "Select deck:")
      (dom/span (str "Decks count: " (count (:decks data)))))))

(defcomponent rules [data owner]
  (render [_]
    (dom/div
     "Game rules")))

(defcomponent not-found [data owner]
  (render [_]
    (dom/div
     "Not found")))
