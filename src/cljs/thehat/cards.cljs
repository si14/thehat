(ns thehat.cards
  (:require-macros
   [thehat.translation :refer [translate]])
  (:require
   [thehat.words :as words]
   [thehat.words-ru :as words-ru]))


(def decks
  (let [m (translate
           :en
           [{:name "Random"
             :words (concat words/cs words/common words/animals
                            words/tv words/food)}
            {:name "Computer Science"
             :words words/cs}
            {:name "Common"
             :words words/common}
            {:name "Animals"
             :words words/animals}
            {:name "Food"
             :words words/food}
            {:name "Screen junkie"
             :words words/tv}]
           :ru
           [{:name "Случайные"
             :words (concat words-ru/common words-ru/war-and-peace)}
            {:name "Война и Мир"
             :words words-ru/war-and-peace}]
           )]
    (->> m
         (map #(assoc % :words-count (count (:words %))))
         (map #(assoc % :background-url
                      (.toDataUrl (.generate js/GeoPattern (:name %)))))
         ;; FIXME(Dmitry): why we need "id" at all?
         (map-indexed (fn [idx x] (assoc x :id idx)))
         (into []))))
