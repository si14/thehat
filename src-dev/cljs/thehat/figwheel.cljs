(ns thehat.figwheel
  (:require
    [figwheel.client :as fw :include-macros true]
    [cljs.core.async :as async :refer [put!]]
    [om.core :as om :include-macros true]
    [thehat.components :refer [game]]
    [thehat.core :refer [run]]))

(run)

(fw/watch-and-reload
 :jsload-callback  (fn []
                     (run)))
