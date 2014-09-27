(ns thehat.helpers
 (:require [goog.string :as gstring]
           [om-tools.dom :as dom :include-macros true]
           ))

(defn escape [s] (gstring/unescapeEntities s))
(def nbsp (escape "&nbsp"))


(def twitter 
  (dom/a {:class "twitter-hashtag-button " 
          :href "https://twitter.com/intent/tweet?button_hashtag=thehat&text=I%20just%20played!" 
          :data-size="large" 
          :data-url="http://playthehat.com"} 
         "Tweet #thehat"))
