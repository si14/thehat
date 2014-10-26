(ns thehat.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [put! chan alts!]]
   [dommy.core :as dommy :refer-macros [sel1]]
   [reagent.core :as reagent :refer [atom]]
   [plumbing.core :as p]
   [thehat.cards :refer [decks]]
   #_[thehat.components :refer [game rules not-found]]
   [thehat.utils :refer [ctg nbsp]]
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
(defonce current-deck (atom nil))
(defonce current-scores (atom [0 0]))
(defonce current-team (atom 0))

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
             :on-click #(put! interaction-chan
                              {:type :deck-click
                               :deck-id id})}
       [:div.inside.rotated "&nbsp;"]
       [:div.inside {:style {:background-image background-url}}
        [:div.word name
         [:div.small words-count " words"]]]])]])

(defn prelude []
  [:div
   [:div.finished {:on-click #(put! interaction-chan
                                    {:type :prelude-click})}
    [:div.big [:span.icon-flag]
     [:div "Ready to start!"]
     [:div.small
      [:span.mobile "Tap"]
      [:span.desktop "Click"]
      " anywhere to start the game"]]]])

(defn round-buttons-one []
  (let [team @current-team]
    [:div.buttons
     [:div.small nbsp]
     [:span.icon.icon-cancel-2.bt-wrong
      {:on-click #(put! interaction-chan
                        {:type :wrong-card
                         :team team})}]
     [:span nbsp]
     [(case @current-team
        0 :span.icon.icon-checkmark.bt-right-team-0
        1 :span.icon.icon-checkmark.bt-right-team-1)
      {:on-click #(put! interaction-chan
                        {:type :right-card
                         :team team})}]]))

(defn round-buttons-both []
  (let [team @current-team]
    [:div.buttons
     ;; FIXME(Dmitry): next 5 lines are identical in both components
     [:div.small nbsp]
     [:span.icon.icon-cancel-2.bt-wrong
      {:on-click #(put! interaction-chan
                        {:type :wrong-card
                         :team team})}]
     [:span nbsp]
     [:span.icon.icon-checkmark.bt-right-team-0
      {:on-click #(put! interaction-chan
                        {:type :right-card
                         :team 0})}]
     [:span nbsp]
     [:span.icon.icon-checkmark.bt-right-team-1
      {:on-click #(put! interaction-chan
                        {:type :right-card
                         :team 1})}]]))

(defn round []
  (let [time @current-time
        deck @current-deck
        progress-width (str (* 100 (/ time round-duration)) "%")
        word (first (:words deck))
        ]
    [:div.game
     [:div.time
      (if (pos? time)
        [:div.progress.active
         {:style {:width progress-width}}
         (int time)]
        nbsp)]
     [:div#rotated-card.card-inner.card-rotated
      {:style {:background-image (:background-url deck)}}
      nbsp]
     ;; FIXME(Dmitry): add animation through React's mechanism
     ;; NOTE(Dmitry): this metadata helps React to infer that the whole
     ;;               subtree should be rerendered
     ^{:key word}
     [:div#current-card.card-inner.animated.bounceInLeft
      [:div.word word
       [:div.buttons
        (if (pos? time)
          [round-buttons-one]
          [round-buttons-both])]]]
     [:div.teams
      [:div.team
       [:div.arrow.a1 [:span.icon-arrow-right]]
       [:div.team1
        {:style {:width (str
                         (->> 0.5 #_(/ score default-max-score)
                              (* 85)
                              (+ 5))
                         "%")}}
        42]]


      ]]))

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
        deck (get decks deck-id)
        shuffled-deck (update-in deck [:words] shuffle)
        animation-end-handler
        (fn [e]
          (reset! current-deck shuffled-deck)
          (reset! current-screen :prelude))]
    (dommy/remove-class! el "flipInX")
    (dommy/add-class! el "bounce")
    (listen-animation-end! el animation-end-handler)))

(defmethod interaction [:prelude :prelude-click]
  [_]
  (reset! current-time (:round-duration config))
  (reset! current-screen :round))

(defmethod interaction [:round :wrong-card]
  [{:keys [team]}]
  (swap! current-scores update-in [team] #(if (pos? %) (dec %) %))
  (swap! current-deck update-in [:words] rest))

(defmethod interaction [:round :right-card]
  [{:keys [team]}]
  (swap! current-scores update-in [team] inc)
  (swap! current-deck update-in [:words] rest))

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
