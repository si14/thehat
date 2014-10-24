(ns thehat.core
  (:require
   [dommy.core :as dommy :refer-macros [sel1]]
   [om.core :as om :include-macros true]
   [om-tools.dom :as dom :include-macros true]
   [om-tools.core :refer-macros [defcomponent defcomponentk]]
   [plumbing.core :as p]
   [thehat.cards :refer [decks]]
   #_[thehat.components :refer [game rules not-found]]
   [thehat.notification :as notification]))

(enable-console-print!)

(def config
  {:round-duration 30
   :max-score 42})

(defn listen-animation-end! [el handler]
  ;; NOTE(Dmitry): to my surprise, at least in Chrome case matters
  (doseq [event-name ["animationEnd" "webkitAnimationEnd" "msAnimationEnd"]]
    (dommy/listen-once! el event-name handler)))

(defn build-deck-click-handler [cursor id]
  (fn []
    (let [el (sel1 (str "#deck_" id))
          animation-end-handler
          (fn [e]
            (om/update! cursor [:game :deck-id] id)
            (om/update! cursor :current-screen :round))]
      (dommy/remove-class! el "flipInX")
      (dommy/add-class! el "bounce")
      (listen-animation-end! el animation-end-handler))))

(defcomponentk deck-chooser
  [[:data :as cursor]]
  (render [_]
    (dom/div {:class "chooser"}
      (dom/div {:class "chooser-inner"}
        (dom/div {:class "title"}
          "Choose a deck:")
        (for [{:keys [name id words-count]} decks]
          (dom/div {:id (str "deck_" id)
                    :class "pack animated flipInX"
                    :on-click (build-deck-click-handler cursor id)}
            (dom/div {:class "inside rotated"} "&nbsp;")
            (dom/div {:class "inside"
                      :style {:background-image
                              (.toDataUrl (.generate js/GeoPattern name))}}
              (dom/div {:class "word"}
                name
                (dom/div {:class "small"} words-count " words" )))))))))

(defcomponentk round
  [[:data :as cursor]]
  (render [_]
    (dom/div "round "
      (dom/a {:href "#"
              :onclick #(om/update! cursor :current-screen :deck-chooser)}
         "back"))))

(defcomponentk interlude
  []
  (render [_]
    (dom/div "interlude")))

(defcomponentk final
  []
  (render [_]
    (dom/div "final")))

(def screens
  {:deck-chooser deck-chooser
   :round round
   :interlude interlude
   :final final})

(defonce app-state
  (atom {:current-screen :deck-chooser
         :game {:time (:round-duration config)
                :deck-id 0
                :scores [0 0]}}))

(defcomponentk root
  [[:data current-screen :as data]]
  (render [_]
    (om/build (p/safe-get screens current-screen) data)))

(defn ^:export run []
  (when (notification/is-ios?)
    (notification/unlock-notification))
  (om/root root app-state {:target (sel1 :#app)}))

;; Local Variables:
;; mode: clojure
;; eval: (put-clojure-indent 'render 1)
;; eval: (put-clojure-indent 'did-update 1)
;; eval: (put-clojure-indent 'section 1)
;; eval: (put-clojure-indent 'div 1)
;; eval: (put-clojure-indent 'span 1)
;; End:
