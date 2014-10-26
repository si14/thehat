(ns thehat.utils)

(def ctg (-> js/React
             (aget "addons")
             (aget "CSSTransitionGroup")))
