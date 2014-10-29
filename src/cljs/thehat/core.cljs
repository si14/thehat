(ns thehat.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
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
  {
   ;; :round-duration 30
   :round-duration 10
   ;; :max-score 24
   :max-score 5
   :sound-warning-percent 0.67 ;; should be in sync with progressRunDown in CSS
   :team-names ["Blue" "Green"]})

;;
;; State
;;

(defonce current-screen (atom :deck-chooser))
(defonce current-time-left (atom (:round-duration config)))
(defonce current-deck (atom nil))
(defonce current-scores (atom [0 0]))
(defonce current-team (atom 0))
(defonce current-round (atom 0))

(defonce interaction-chan (chan))
(defonce timer-stop-chan (chan))

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
             :class "pack"
             :on-click #(put! interaction-chan
                              {:type :deck-click
                               :deck-id id})}
       [:div.inside.rotated "&nbsp;"]
       [:div.inside {:style {:background-image background-url}}
        [:div.word name
         [:div.small words-count " words"]]]])]])

(defn prelude []
  [:div.finished {:on-click #(put! interaction-chan
                                   {:type :prelude-click})}
   [:div.big [:span.icon-flag]]
   [:div "Ready to start!"]
   [:div.small
    [:span.mobile "Tap"]
    [:span.desktop "Click"]
    " anywhere to start the game"]])

(defn round-buttons [both?]
  (let [team @current-team]
    [:div.buttons
     (if both?
       [:div.small "Both teams can guess now"]
       [:div.small nbsp])
     [:span.icon.icon-cancel-2.bt-wrong
      {:on-click #(put! interaction-chan
                        {:type :wrong-card
                         :team team})}]
     [:span nbsp]
     (if both?
       (seq
        [^{:key :team-0}
         [:span.icon.icon-checkmark.bt-right-team-0
          {:on-click #(put! interaction-chan
                            {:type :right-card
                             :team 0})}]
         ^{:key :nbsp-0}
         [:span nbsp]
         ^{:key :team-1}
         [:span.icon.icon-checkmark.bt-right-team-1
          {:on-click #(put! interaction-chan
                            {:type :right-card
                             :team 1})}]])
       ^{:key (case team 0 :team-0 1 :team-1)}
       [(case team
          0 :span.icon.icon-checkmark.bt-right-team-0
          1 :span.icon.icon-checkmark.bt-right-team-1)
        {:on-click #(put! interaction-chan
                          {:type :right-card
                           :team team})}])]))

(defn team [active? arrow-class score-class score]
  (let [offset (str (- (* 93 (/ score (:max-score config))) 93) "%")
        translation (str "translate3d(" offset ",0,0)")]
    [:div.team {:class (when-not active? "inactive")}
     [:div.arrow {:class arrow-class}
     [:span.icon-arrow-right]]
    [:div.score {:class score-class
                 :style {:-webkit-transform translation
                         :transform translation}}
     [:span.label score]]]))

(defn teams []
  (let [[score-0 score-1] @current-scores
        active-team @current-team]
    [:div.teams
     [team (= active-team 0) :arrow-0 :score-0 score-0]
     [team (= active-team 1) :arrow-1 :score-1 score-1]]))

(defn progress-plain [round-n]
  (let [time @current-time-left]
    [:div.time
     (if (pos? time)
       [:div.progress.active
        {:style
         {:-webkit-animation-duration (str (:round-duration config) "s")
          :animation-duration (str (:round-duration config) "s")}}
        [:span.label (int time)]]
       nbsp)]))

(def progress
  (with-meta progress-plain
    ;; FIXME(Dmitry): move notification/start to ticker
    {:component-did-mount
     (fn [_] (notification/start
              (:round-duration config)
              (:sound-warning-percent config)))
     :component-will-unmount
     (fn [_] (notification/stop))}))

(defn round []
  (let [time @current-time-left
        deck @current-deck
        round-n @current-round
        word (first (:words deck))]
    [:div.game
     ;; NOTE(Dmitry): prevent reuse of this DOM element between rounds
     [progress round-n]
     [:div#rotated-card.card-inner.card-rotated
      {:style {:background-image (:background-url deck)}}
      nbsp]
     [ctg {:transitionName "card"}
      ;; NOTE(Dmitry): this metadata helps React to infer that the whole
      ;;               subtree should be rerendered
      ^{:key word}
      [:div#current-card.card-inner.keyframe-animated
       [:div.word word
        [round-buttons (not (pos? time))]]]]
     [teams]]))

(defn interlude []
  [:div
   [:div.finished {:on-click #(put! interaction-chan
                                    {:type :interlude-click})}
    [:div.big [:span.icon-flag]]
    [:div "Round finished!"]
    [:div.small
     [:span.mobile "Tap"]
     [:span.desktop "Click"]
     " anywhere and give another team a chance"]]])

(defn final []
  (let [scores @current-scores
        winner (if (> (first scores) (second scores)) 0 1)
        winner-name (get (:team-names config) winner)
        winner-class (str "winner-team-" winner)]
    [:div
     [:div.finished {:on-click #(put! interaction-chan
                                      {:type :final-click})}
      [:div.big [:span.icon-flag]]
      [:div
       [:span {:class winner-class} winner-name]
       " team won!"]
      [:div.small
       [:span.mobile "Tap"]
       [:span.desktop "Click"]
       " anywhere to start new game"]]]))

(def screens
  {:deck-chooser deck-chooser
   :prelude prelude
   :round round
   :interlude interlude
   :final final})

(defn root []
  (let [screen @current-screen]
     ^{:key screen}
     [(p/safe-get screens @current-screen)]))

;;
;; Interaction
;;

(defmulti interaction (fn [x] [@current-screen (:type x)]))

(defmethod interaction [:deck-chooser :deck-click]
  [{:keys [deck-id]}]
  (let [deck (get decks deck-id)
        shuffled-deck (update-in deck [:words] shuffle)]
    (reset! current-deck shuffled-deck)
    (reset! current-screen :prelude)))

(defn start-ticker []
  (let [max-time (:round-duration config)
        start-ts (js/Date.now)]
    (go-loop [time 0]
      (let [new-time (inc time)
            to-sleep (- (+ start-ts (* 1000 new-time))
                        (js/Date.now))
            to (async/timeout (max to-sleep 0))
            [_ ch] (alts! [timer-stop-chan to])]
        (when (and (= ch to)
                   (<= new-time max-time))
          (do (reset! current-time-left (- max-time new-time))
              (recur new-time)))))))

(defmethod interaction [:prelude :prelude-click]
  [_]
  (reset! current-team (if (> (js/Math.random) 0.5) 0 1))
  (reset! current-scores [0 0])
  (reset! current-time-left (:round-duration config))
  (reset! current-round 0)
  (reset! current-screen :round)
  (start-ticker))

(defmethod interaction [:round :wrong-card]
  [{:keys [team]}]
  (dommy/add-class! (sel1 :#current-card) :wrong)
  (swap! current-scores update-in [team] #(if (pos? %) (dec %) %))
  (if (pos? @current-time-left)
    (swap! current-deck update-in [:words] rest)
    (reset! current-screen :interlude)))

(defmethod interaction [:round :right-card]
  [{:keys [team]}]
  (dommy/add-class! (sel1 :#current-card) :right)
  (let [scores (swap! current-scores update-in [team] inc)]
    (if (or (>= (get scores team) (:max-score config))
            (<= (count (:words @current-deck)) 0))
      (reset! current-screen :final)
      (if (pos? @current-time-left)
        (swap! current-deck update-in [:words] rest)
        (reset! current-screen :interlude)))))

(defmethod interaction [:interlude :interlude-click]
  [{:keys [team]}]
  (swap! current-team #(- 1 %))
  (swap! current-deck update-in [:words] rest)
  (swap! current-round inc)
  (reset! current-time-left (:round-duration config))
  (reset! current-screen :round)
  (start-ticker))

(defmethod interaction [:final :final-click]
  [_]
  (reset! current-screen :deck-chooser))

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
