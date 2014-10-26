(ns thehat.utils
  (:require [goog.string :as gstring]))

(def ctg (-> js/React
             (aget "addons")
             (aget "CSSTransitionGroup")))

(defn escape [s] (gstring/unescapeEntities s))
(def nbsp (escape "&nbsp;"))
