(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defcomponent game [data owner]
  (render [_]
    (dom/div
     (dom/a {:class "back" :on-click #(secretary/dispatch! "#/")} "back"))))

(defcomponent new-game [data owner]
  (render [_]
    (dom/div
     (dom/a {:class "back" :on-click #(secretary/dispatch! "#/game")} "start"))))
