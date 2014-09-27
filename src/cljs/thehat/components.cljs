(ns thehat.components
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentk]]
            [thehat.components.game-init :refer [game-init]]
            [thehat.components.game-process :refer [game-process]]
            [cljs.core.async :as async :refer [<! >! chan close! put!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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
