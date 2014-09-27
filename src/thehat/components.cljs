(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [clojure.string :refer [join]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defcomponent game-process [{:keys [cards]
                             :as data} owner]
  (render [_]
    (dom/div
     (join ", " cards))))

(defcomponent game-init [{:keys [game-ch]
                          :as data} owner]
  (render [_]
    (dom/a {:class "back" :on-click #(secretary/dispatch! "#/")} "back")
    (dom/button {:on-click #(put! game-ch {:component game-process
                                           :args {:cards ["HELLO" "WORLD"]}})}
                "start")))

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
