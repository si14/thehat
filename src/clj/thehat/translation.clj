(ns thehat.translation
  (:require
   [environ.core :refer [env]]
   [plumbing.core :refer [safe-get]]))

(def default-lang :ru)

(defn iae [arg]
  (throw (IllegalArgumentException. arg)))

(defn current-lang []
  (if-let [s (env :translation)]
    (keyword s)
    default-lang))

(defmacro translate [& lang-pairs]
  (when-not (= 0 (rem (count lang-pairs) 2))
    (iae "translate takes an even number of arguments"))
  (safe-get (apply hash-map lang-pairs) (current-lang)))
