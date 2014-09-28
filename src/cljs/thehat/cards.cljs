(ns thehat.cards
  (:require [thehat.words :as words]))

(def decks [{:id 1
             :name "Random"
             :words (concat words/cs words/common words/animals
                            words/clothes words/food)}
            {:id 2
             :name "Computer Science"
             :words words/cs}
            {:id 3
             :name "Common"
             :words words/common}
            {:id 4
             :name "Animals"
             :words words/animals}
            {:id 6
             :name "Food"
             :words words/food}
            {:id 7
             :name "Screen junkie"
             :words words/tv}])
