(ns thehat.figwheel
  (:require
    [figwheel.client :as fw :include-macros true]
    [thehat.core :refer [run]]))

(run)

(fw/watch-and-reload
 :jsload-callback (fn [] (run)))
