(ns thehat.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [thehat.components :refer [game rules not-found]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [cljs.core.async :as async :refer [<! >! chan close! put!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:import goog.History))

(enable-console-print!)
(secretary/set-config! :prefix "#")
(def route-ch (chan))

;; TODO: get from server
(def decks [{:id 1 :name "All" :words ["hello" "world"]}
            {:id 2 :name "Animals" :words ["why" "guys" "you"]}
            {:id 3 :name "Temp" :words ["so" "long" "and" "longer"]}
            {:id 4 :name "Food" :words ["cola" "russian" "bear" "beer"]}
            {:id 5 :name "Seas" :words ["cola" "russian" "bear" "beer"]}
            {:id 6 :name "Drinks!" :words ["cola" "russian" "bear" "beer"]}
            ])

(def app-state (atom {:decks decks}))

(defcomponent root [data owner]
  (init-state [_]
    {:component nil
     :args {}})
  (will-mount [_]
    (go-loop []
      (let [{:keys [component args]
             :as c} (<! route-ch)]
        (om/set-state! owner c)
        (recur))))
  (render-state [_ {:keys [component args]
                    :as s}]
    (when component
      (om/build component (merge data args {:route-ch route-ch})))))


(defroute "/" []
  (put! route-ch {:component game}))
(defroute "/rules" []
  (put! route-ch {:component rules}))
(defroute "*" []
  (put! route-ch {:component not-found}))


(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))

(defn run []
  (om/root root app-state
           {:target (. js/document (getElementById "app"))}))