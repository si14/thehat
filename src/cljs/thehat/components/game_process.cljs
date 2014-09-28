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

(def default-max-time 7)
(def default-max-score 3)
(defn to-game-init [ch] #(put! ch {:component :game-init :args {}}))

(defn get-words [id decks]
  (some #(when (= id (:id %)) (:words %)) decks))

(defn next-team [t]
  (condp = t
    :team-1 :team-2
    :team-2 :team-1))

(defn animate-card-out
  [div owner current-round words s]
  (fn []
    (dommy/remove-class! div "bounceInLeft")
    (dommy/add-class! div "bounceOutRight")
    (->> (fn [e] (do
                   (om/update-state!
                    owner
                    #(assoc %
                       current-round (inc (get s current-round))
                       :words (into [] (drop 1 words))))
                   (dommy/remove-class! div "bounceOutRight")
                   (dommy/add-class! div "bounceInLeft")))
         (dommy/listen! div :webkitAnimationEnd))))

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
                   :on-click (fn []
                               (om/update-state!
                                owner #(assoc %
                                         current-round (max 0 (dec (get s current-round)))
                                         :words (into [] (drop 1 words)))))})
        (dom/span nbsp)
        (dom/span {:class (string/join " " ["icon"
                                            "icon-checkmark"
                                            (str "bt-right-"
                                                 (clojure.core/name current-round))])
                   :on-click (animate-card-out
                              (sel1 :#current-card) owner current-round words s)}))

       (dom/div
        {:class "buttons"}
        (dom/div {:class "small"} "both teams can guess now")
        (dom/span {:class "icon icon-cancel-2 bt-wrong"
                   :on-click (fn []
                                 (om/update-state!
                                  owner #(assoc %
                                           current-round (max 0 (dec (get s current-round)))
                                           :words (into [] (drop 1 words))
                                           :current-round :pause)))})
        (dom/span nbsp)
        (dom/span {:class "icon icon-checkmark bt-right-team-1"
                   :on-click (comp
                              #(om/set-state! owner :current-round :pause)
                              (animate-card-out
                               (sel1 :#current-card) owner :team-1 words s))})
        (dom/span nbsp)
        (dom/span {:class "icon icon-checkmark bt-right-team-2"
                   :on-click (comp
                              #(om/set-state! owner :current-round :pause)
                              (animate-card-out
                               (sel1 :#current-card) owner :team-2 words s))})))))

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
                    " anywhere to return to the package list.")))

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
  (did-mount [_] (. js/window initFacebook))
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
