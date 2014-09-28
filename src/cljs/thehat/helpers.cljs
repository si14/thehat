(ns thehat.helpers
 (:require [goog.string :as gstring]
           [om-tools.dom :as dom :include-macros true]))

;; Escaping

(defn escape [s] (gstring/unescapeEntities s))
(def nbsp (escape "&nbsp"))

;; Device orientation

(defn is-landscape-angle? [angle] (= (Math/abs angle) 90))

(defn handle-new-orientation
  []
  (let [angle (or (.-orientation js/window) 0)
        css-class (if (is-landscape-angle? angle) "landscape" "portrait")]
    (. (.-body js/document) (setAttribute "class" css-class))))

(. js/window addEventListener "orientationchange" handle-new-orientation)
(handle-new-orientation)

;; Sharing

(def twitter
  (dom/a {:class "twitter-hashtag-button "
          :href "https://twitter.com/intent/tweet?button_hashtag=thehat&text=I%20just%20played!"
          :data-size="large"
          :data-url="http://playthehat.com"}
         "Tweet #thehat"))
(def facebook
  (dom/div {:class "fb-like"
          :data-href "http://playthehat.com"
          :data-layout "button_count"
          :data-action "like"
          :data-show-faces "true"
          :data-share "false"
          }))
