(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [clojure.string :refer [join]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defcomponent deck [data owner]
  (render [_]
    (dom/div {:class "deck" :data-deck-id (:id data)} (:name data))))

(defcomponent game-process [{:keys [decks]
                             :as data} owner]
  (render [_]
    (dom/div
     (dom/span (str "Decks count: " (count (:decks data))))
     (dom/span (join ", " decks)))))

(defcomponent game-init [{:keys [game-ch]
                          :as data} owner]
  (render [_]
    (dom/a {:class "back" :on-click #(secretary/dispatch! "#/")} "back")
    (dom/button {:on-click #(put! game-ch {:component game-process
                                           :args {:decks ["HELLO" "WORLD"]}})}
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
