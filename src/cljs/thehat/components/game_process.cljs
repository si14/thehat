(ns thehat.components.game-process
  (:require [clojure.string :as string]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [cljs.core.async :as async :refer [put!]]
            [dommy.core :as dommy]
            [thehat.helpers :as h :refer [nbsp]]
            [thehat.notification :as notification])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(def default-max-time 30)
(def default-max-score 72)
(defn to-game-init [ch] #(put! ch {:component :game-init :args {}}))

(defn get-words [id decks]
  (->> decks
       (some #(when (= id (:id %)) (:words %)))
       (shuffle)))

(defn next-team [t]
  (condp = t
    :team-1 :team-2
    :team-2 :team-1))

(defn card-right
  [div owner s team words set-pause?]
  (fn []
    (dommy/remove-class! div "bounceInLeft")
    (dommy/add-class! div "bounceOutRight")
    (om/update-state! owner team inc)
    (->> (fn [e] (do
                   (om/update-state! owner :words #(into [] (drop 1 %)))
                   (dommy/remove-class! div "bounceOutRight")
                   (dommy/add-class! div "bounceInLeft")
                   (if set-pause?
                     (om/set-state! owner :current-round :pause))))
         (dommy/listen-once! div :webkitAnimationEnd))))

(defn card-wrong
  [div owner s team words set-pause?]
  (fn []
    (dommy/remove-class! div "bounceInLeft")
    (dommy/add-class! div "bounceOutLeft")
    (om/update-state! owner team #(max 0 (dec %)))
    (->> (fn [e] (do
                   (om/update-state! owner :words #(into [] (drop 1 %)))
                   (dommy/remove-class! div "bounceOutLeft")
                   (dommy/add-class! div "bounceInLeft")
                   (if set-pause?
                     (om/set-state! owner :current-round :pause))))
         (dommy/listen-once! div :webkitAnimationEnd))))

(defn clear-interval [owner]
  (let [interval (-> (om/get-state owner)
                     (:interval))]
    (when interval
      (js/clearInterval interval)))
  (om/set-state! owner :interval nil))

(defn interval [owner]
  (let [round-seq (-> (om/get-state owner)
                      (:round-seq))
        {:keys [name time]} (first round-seq)]
    (om/update-state!
     owner
     (fn [s]
       (assoc s
         :current-round name
         :time time
         :max-time time
         :round-seq (rest round-seq)
         :interval (js/setInterval
                    (fn []
                      (let [{:keys [time current-round round-seq]} (om/get-state owner)]
                        (if (> time 0)
                          (om/update-state! owner :time #(- % 0.25)))))
                    250))))))

(defn state-in-progress [owner name {:keys [team-1 team-2 words current-round
                                            time max-time max-words] :as s}]
  ;; FIXME(Dmitry): it's better to save "notified" state in state and check
  ;; less-than here
  (when (= time 5)
    (notification/start-notifying))

  (dom/div
   {:class "game"}
   (dom/div
    {:class "time"}
    (if (> time 0)
      (dom/div
       {:class "progress active"
        :style {:width (str (* 100 (/ time max-time)) "%")}} (int time))
      nbsp))

   (dom/div
    {:class "card-inner card-rotated"
     :id "rotated-card"
     :style {:background-image (.toDataUrl (.generate js/GeoPattern name))}}
    nbsp)

   (dom/div
    {:class "card-inner card-inner bounceInLeft animated" :id "current-card"}
    (dom/div
     {:class "word"}
     (first words)

     (if (> time 0)
       (dom/div
        {:class "buttons"}
        (dom/div {:class "small"} nbsp)
        (dom/span {:class "icon icon-cancel-2 bt-wrong"
                   :on-click (card-wrong (sel1 :#current-card) owner s current-round words false)})
        (dom/span nbsp)
        (dom/span {:class (string/join " " ["icon"
                                            "icon-checkmark"
                                            (str "bt-right-"
                                                 (clojure.core/name current-round))])
                   :on-click (card-right (sel1 :#current-card) owner s current-round words false)}))
       (dom/div
        {:class "buttons"}
        (dom/div {:class "small"} "both teams can guess now")
        (dom/span {:class "icon icon-cancel-2 bt-wrong"
                   :on-click (fn []
                               (om/update-state!
                                owner
                                #(assoc %
                                   :current-round :pause
                                   :words (into [] (drop 1 (:words %))))))})
        (dom/span nbsp)
        (dom/span {:class "icon icon-checkmark bt-right-team-1"
                   :on-click (card-right (sel1 :#current-card) owner s :team-1 words true)})
        (dom/span nbsp)
        (dom/span {:class "icon icon-checkmark bt-right-team-2"
                   :on-click (card-right (sel1 :#current-card) owner s :team-2 words true)})))))

   (dom/div
    {:class "teams"}
    (dom/div
     {:class (str "team" (if (= current-round :team-2) " inactive" ""))}
     (dom/div {:class "arrow a1"}
              (dom/span {:class "icon-arrow-right"}))
     (dom/div {:class "team1"
              :style {:width (str
                              (->> (/ team-1 default-max-score)
                                   (* 90)
                                   (+ 5))
                              "%")}} team-1))
    (dom/div
     {:class (str "team" (if (= current-round :team-1) " inactive" ""))}
     (dom/div {:class "arrow a2"} (dom/span {:class "icon-arrow-right"}))
     (dom/div {:class "team2"
              :style {:width (str
                              (->> (/ team-2 default-max-score)
                                   (* 90)
                                   (+ 5))
                              "%")}} team-2)))))

(defn state-pause [owner]
  (dom/div {:class "finished" :on-click #(interval owner)}
           (dom/div {:class "big"} (dom/span {:class "icon-flag"}))
           (dom/div "Round finished!")
           (dom/div {:class "small"}
                    (dom/span {:class "mobile"} "Tap")
                    (dom/span {:class "desktop"} "Click")
                    " anywhere and give another team a chance.")))

(defn state-final-score [owner {:keys [team-1 team-2 game-ch]}]
  (dom/div {:class "finished" :on-click (to-game-init game-ch)}
           (dom/div {:class "big"} (dom/span {:class "icon-flag"}))
           (dom/div (case (compare team-1 team-2)
                      1 "Blue team won!"
                      -1 "Green team won!"
                      0 "Draw!"))
           (dom/div {:class "small"}
                    (dom/span {:class "mobile"} "Tap")
                    (dom/span {:class "desktop"} "Click")
                    " anywhere to return to the deck list.")
           (dom/div {:class "sharing"}
                    h/twitter
                    h/google
                    h/facebook)))

(defcomponentk game-process [[:data deck-id decks game-ch name :as data] owner]
  (init-state [_]
    {:interval nil
     :team-1 0
     :team-2 0
     :max-time 0
     :max-words 10
     :round-seq (cycle [{:name :team-1
                         :time default-max-time}
                        {:name :team-2
                         :time default-max-time}])
     :current-round nil
     :words (into [] (get-words deck-id decks))
     :game-ch game-ch})
  (will-mount [_]
    (interval owner))
  (did-mount [_] (. js/window initFacebook) (h/init-google))
  (render-state [_ {:keys [words time current-round interval team-1 team-2]
                    :as s}]
    (cond
     (or
      (>= team-1 default-max-score)
      (>= team-2 default-max-score)) (do
                                       (clear-interval owner)
                                       (state-final-score owner s))
     (= current-round :pause) (do
                                (clear-interval owner)
                                (state-pause owner))
     (> (count words) 0) (state-in-progress owner name s)
     (= (count words) 0) (do
                           (clear-interval owner)
                           (state-final-score owner s))
     :else (do
             (clear-interval owner)
             (state-final-score owner s)))))
