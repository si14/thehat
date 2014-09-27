(ns thehat.cards)

(declare decks)

(defn get-decks [] (map #(assoc % :words (-> % :words shuffle)) decks))

(def decks [{:id 1 :name "All" :words ["hello" "world"]}
            {:id 2 :name "Animals" :words ["why" "guys" "you"]}
            {:id 3 :name "Temp" :words ["so" "long" "and" "longer"]}
            {:id 4 :name "Food" :words ["cola" "russian" "bear" "beer"]}
            {:id 5 :name "Seas" :words ["cola" "russian" "bear" "beer"]}
            {:id 6 :name "Drinks!" :words ["cola" "russian" "bear" "beer"]}
            ])