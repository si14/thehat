(ns thehat.cards
  (:require [thehat.words :as words]))

(declare decks)
(defn get-decks [] (map #(assoc % :words (-> % :words shuffle)) decks))

(def decks [{:id 1
             :name "Random"
             :words (concat words/cs words/common words/animals
                            words/clothes words/art words/food)}
            {:id 2
             :name "Computer Science"
             :words words/cs}
            {:id 3
             :name "Common"
             :words words/common}
            {:id 4
             :name "Animals"
             :words words/animals}
            {:id 5
             :name "Clothes"
             :words words/clothes}
            {:id 6
             :name "Food"
             :words words/food}])
