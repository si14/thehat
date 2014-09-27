(ns thehat.figwheel
  (:require 
    [figwheel.client :as fw :include-macros true]
    [cljs.core.async :as async :refer [put!]]
    [thehat.components :refer [game]]
    [thehat.core :refer [app-state route-ch run]]))

(run)

(fw/watch-and-reload
 :jsload-callback  (fn []
                     (run)
                     (reset! app-state @app-state)
                     (put! route-ch {:component game})))
