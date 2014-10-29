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
   ;; :max-score 42
   :max-score 5
   :team-names ["Blue" "Green"]})

;;
;; State
;;

(defonce current-screen (atom :deck-chooser))
(defonce current-time (atom (:round-duration config)))
(defonce current-deck (atom nil))
(defonce current-scores (atom [0 0]))
(defonce current-team (atom 0))

(defonce interaction-chan (chan))
(defonce timer-stop-chan (chan))

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
  [:div.finished {:on-click #(put! interaction-chan
                                   {:type :prelude-click})}
   [:div.big [:span.icon-flag]
    [:div "Ready to start!"]
    [:div.small
     [:span.mobile "Tap"]
     [:span.desktop "Click"]
     " anywhere to start the game"]]])

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

(defn team [active? arrow-class score-class score]
  (let [offset (str (- (* 95 (/ score (:max-score config))) 95) "%")
        translation (str "translate3d(" offset ",0,0)")]
    [:div.team {:class (when-not active? "inactive")}
     [:div.arrow {:class arrow-class}
     [:span.icon-arrow-right]]
    [:div.score {:class score-class
                 :style {:transform translation}}
     [:span.label score]]]))

(defn teams []
  (let [[score-0 score-1] @current-scores
        active-team @current-team]
    [:div.teams
     [team (= active-team 0) :arrow-0 :score-0 score-0]
     [team (= active-team 1) :arrow-1 :score-1 score-1]]))

(defn round []
  (let [time @current-time
        deck @current-deck
        progress-width (str (- 100 (* 100 (/ time (:round-duration config))))
                            "%")
        progress-translation (str "translate3d(-" progress-width ",0,0)")
        word (first (:words deck))]
    [:div.game
     [:div.time
      (if (pos? time)
        [:div.progress.active
         {:style {:transform progress-translation}}
         [:span.label (int time)]]
        nbsp)]
     [:div#rotated-card.card-inner.card-rotated
      {:style {:background-image (:background-url deck)}}
      nbsp]
     [ctg {:transitionName "card"}
      ;; NOTE(Dmitry): this metadata helps React to infer that the whole
      ;;               subtree should be rerendered
      ^{:key word}
      [:div#current-card.card-inner.keyframe-animated
       [:div.word word
        [:div.buttons
         (if (pos? time)
           [round-buttons-one]
           [round-buttons-both])]]]]
     [teams]]))

(defn interlude []
  [:div
   [:div.finished {:on-click #(put! interaction-chan
                                    {:type :interlude-click})}
    [:div.big [:span.icon-flag]
     [:div "Round finished!"]
     [:div.small
      [:span.mobile "Tap"]
      [:span.desktop "Click"]
      " anywhere and give another team a chance"]]]])

(defn final []
  (let [scores @current-scores
        winner (if (> (first scores) (second scores)) 0 1)
        winner-name (get (:team-names config) winner)
        winner-class (str "winner-team-" winner)]
    [:div
     [:div.finished {:on-click #(put! interaction-chan
                                      {:type :final-click})}
      [:div.big [:span.icon-flag]
       [:div
        [:span {:class winner-class} winner-name]
        " team won!"]
       [:div.small
        [:span.mobile "Tap"]
        [:span.desktop "Click"]
        " anywhere to start new game"]]]]))

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
  (let [el (sel1 (str "#deck_" deck-id))
        deck (get decks deck-id)
        shuffled-deck (update-in deck [:words] shuffle)
        animation-end-handler
        (fn [e]
          (reset! current-deck shuffled-deck)
          (reset! current-screen :prelude))]
    (dommy/remove-class! el "flipInX")
    #_(dommy/add-class! el "bounce")
    (animation-end-handler)
    #_(listen-animation-end! el animation-end-handler)))

(defn start-ticker []
  (go-loop [time (:round-duration config)]
    ;; FIXME(Dmitry): it can be done better to account for time drift
    ;;                (timeout based on actual time)
    (let [to (async/timeout 1000)
          [_ ch] (alts! [timer-stop-chan to])
          new-time (dec time)]
      (when (and (= ch to)
                 (>= new-time 0))
        (do (reset! current-time new-time)
            (recur new-time))))))

(defmethod interaction [:prelude :prelude-click]
  [_]
  (reset! current-team (if (> (js/Math.random) 0.5) 0 1))
  (reset! current-scores [0 0])
  (reset! current-time (:round-duration config))
  (reset! current-screen :round)
  (start-ticker))

(defmethod interaction [:round :wrong-card]
  [{:keys [team]}]
  (dommy/add-class! (sel1 :#current-card) :wrong)
  (swap! current-scores update-in [team] #(if (pos? %) (dec %) %))
  (if (pos? @current-time)
    (swap! current-deck update-in [:words] rest)
    (reset! current-screen :interlude)))

(defmethod interaction [:round :right-card]
  [{:keys [team]}]
  (dommy/add-class! (sel1 :#current-card) :right)
  (let [scores (swap! current-scores update-in [team] inc)]
    (if (or (>= (get scores team) (:max-score config))
            (<= (count (:words @current-deck)) 0))
      (reset! current-screen :final)
      (if (pos? @current-time)
        (swap! current-deck update-in [:words] rest)
        (reset! current-screen :interlude)))))

(defmethod interaction [:interlude :interlude-click]
  [{:keys [team]}]
  (swap! current-team #(- 1 %))
  (swap! current-deck update-in [:words] rest)
  (reset! current-time (:round-duration config))
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
