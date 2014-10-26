(ns thehat.core
  (:require
   [dommy.core :as dommy :refer-macros [sel1]]
   [reagent.core :as reagent :refer [atom]]
   [plumbing.core :as p]
   [thehat.cards :refer [decks]]
   #_[thehat.components :refer [game rules not-found]]
   [thehat.notification :as notification]))

(enable-console-print!)

(def config
  {:round-duration 30
   :max-score 42})

(defonce current-screen (atom :deck-chooser))
(defonce current-time (atom (:round-duration config)))
(defonce deck-id (atom nil))
(defonce scores (atom [0 0]))

(defn listen-animation-end! [el handler]
  ;; NOTE(Dmitry): to my surprise, at least in Chrome case matters
  (doseq [event-name ["animationend"
                      "webkitAnimationEnd"
                      "MSAnimationEnd"
                      "oanimationend"]]
    (dommy/listen-once! el event-name handler)))

(defn build-deck-click-handler [id]
  (fn []
    (let [el (sel1 (str "#deck_" id))
          animation-end-handler
          (fn [e]
            (reset! deck-id id)
            (reset! current-screen :round))]
      (dommy/remove-class! el "flipInX")
      (dommy/add-class! el "bounce")
      (listen-animation-end! el animation-end-handler))))

(defn deck-chooser []
  [:div.chooser
   [:div.chooser-inner
    [:div.title "Choose a deck:"]
    (for [{:keys [name id words-count background-url]} decks]
      ^{:key id} ;; react's key to improve rendering perf
      [:div {:id (str "deck_" id)
             :class "pack animated flipInX"
             :on-click (build-deck-click-handler id)}
       [:div.inside.rotated "&nbsp;"]
       [:div.inside {:style {:background-image background-url}}
        [:div.word name
         [:div.small words-count " words"]]]])]])

(defn round []
  [:div "round "
   [:a {:href "#"
        :on-click #(reset! current-screen :deck-chooser)}
    "back"]])

(defn interlude []
  [:div "interlude"])

(defn final []
  [:div "final"])

(def screens
  {:deck-chooser deck-chooser
   :round round
   :interlude interlude
   :final final})

(def ctg (-> js/React (aget "addons") (aget "CSSTransitionGroup")))

(defn root []
  [ctg {:transitionName "screen"}
   (let [screen @current-screen]
     ^{:key screen}
     [(p/safe-get screens @current-screen)])])

(defn ^:export run []
  (when (notification/is-ios?)
    (notification/unlock-notification))
  (reagent/render-component [root] (sel1 :#app)))
