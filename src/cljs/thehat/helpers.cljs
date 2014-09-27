(ns thehat.helpers
 (:require [goog.string :as gstring]))

(defn escape [s] (gstring/unescapeEntities s))
(def nbsp (escape "&nbsp"))
