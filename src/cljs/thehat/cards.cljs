(ns thehat.cards
  (:require [thehat.words :as words]))

(def decks
  (->> [{:name "Random"
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
       (map #(assoc % :words-count (count (:words %))))
       ;; FIXME(Dmitry): why we need "id" at all?
       (map-indexed (fn [idx x] (assoc x :id idx)))
       (into [])))
