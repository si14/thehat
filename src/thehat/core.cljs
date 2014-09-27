(ns thehat.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [thehat.components :refer [game new-game rules not-found]]
            [figwheel.client :as fw :include-macros true]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:import goog.History))

(enable-console-print!)
(secretary/set-config! :prefix "#")
(def route-ch (chan))

(def app-state (atom {:text "Hello world!"}))

(defcomponent root [data owner]
  (init-state [_]
    {:component nil})
  (will-mount [_]
    (go-loop []
      (let [c (<! route-ch)]
        (om/set-state! owner :component c)
        (recur))))
  (render-state [_ {:keys [component]}]
    (when component
      (om/build component data))))

(fw/watch-and-reload
 :jsload-callback  (fn [] 
                     (reset! app-state @app-state) 
                     (put! route-ch game)))


(defroute "/" []
  (put! route-ch new-game))
(defroute "/game" []
  (put! route-ch game))
(defroute "/rules" []
  (put! route-ch rules))
(defroute "*" []
  (put! route-ch not-found))


(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))


(om/root root app-state
         {:target (. js/document (getElementById "app"))})
