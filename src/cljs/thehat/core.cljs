(ns thehat.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [put! chan alts!]]
   [dommy.core :as dommy :refer-macros [sel1]]
   [reagent.core :as reagent :refer [atom]]
   [plumbing.core :as p]
   [thehat.cards :refer [decks]]
   #_[thehat.components :refer [game rules not-found]]
   [thehat.utils :refer [ctg]]
   [thehat.notification :as notification]))

(enable-console-print!)

(def config
  {:round-duration 30
   :max-score 42})

;;
;; State
;;

(defonce current-screen (atom :deck-chooser))
(defonce current-time (atom (:round-duration config)))
(defonce current-deck-id (atom nil))
(defonce current-scores (atom [0 0]))

(defonce interaction-chan (chan))

;;
;; Utils
;;

;; FIXME(Dmitry): this shit sucks, it's better to remove it somehow
(defn listen-animation-end! [el handler]
  ;; NOTE(Dmitry): to my surprise, at least in Chrome case matters
  (doseq [event-name ["animationend"
                      "webkitAnimationEnd"
                      "MSAnimationEnd"
                      "oanimationend"]]
    (dommy/listen-once! el event-name handler)))

;;
;; Components
;;

(defn deck-chooser []
  [:div.chooser
   [:div.chooser-inner
    [:div.title "Choose a deck:"]
    (for [{:keys [name id words-count background-url]} decks]
      ^{:key id} ;; react's key to improve rendering perf
      [:div {:id (str "deck_" id)
             :class "pack animated flipInX"
             :on-click #(put! interaction-chan {:type :deck-click
                                                :deck-id id})}
       [:div.inside.rotated "&nbsp;"]
       [:div.inside {:style {:background-image background-url}}
        [:div.word name
         [:div.small words-count " words"]]]])]])

(defn prelude []
  [:div
   [:div.finished {:on-click #(reset! current-screen :deck-chooser)}
    [:div.big [:span.icon-flag]
     [:div "Ready to start!"]
     [:div.small
      [:span.mobile "Tap"]
      [:span.desktop "Click"]
      " anywhere to start the game"]]]])

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
   :prelude prelude
   :round round
   :interlude interlude
   :final final})

(defn root []
  [ctg {:transitionName "screen"}
   (let [screen @current-screen]
     ^{:key screen}
     [(p/safe-get screens @current-screen)])])

;;
;; Interaction
;;

(defmulti interaction (fn [x] [@current-screen (:type x)]))

(defmethod interaction [:deck-chooser :deck-click]
  [{:keys [deck-id]}]
  (let [el (sel1 (str "#deck_" deck-id))
        animation-end-handler
        (fn [e]
          (reset! current-deck-id deck-id)
          (reset! current-screen :prelude))]
    (dommy/remove-class! el "flipInX")
    (dommy/add-class! el "bounce")
    (listen-animation-end! el animation-end-handler)))

(defmethod interaction :default
  [arg]
  (prn "don't know what to do with" arg
       "on screen" @current-screen))

(go
  (while true
    (interaction (<! interaction-chan))))

;;
;; Exported stuff
;;

(defn ^:export run []
  (when (notification/is-ios?)
    (notification/unlock-notification))
  (reagent/render-component [root] (sel1 :#app)))
