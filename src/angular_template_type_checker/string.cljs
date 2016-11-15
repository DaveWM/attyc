(ns angular-template-type-checker.string
  (:require [clojure.string :as str]))

(defn split-by-non-repeated [string character]
  "splits a string by a character, as long as the character is not repeated"
  (-> string
      (str/replace (apply str (repeat 2 character)) "##")
      (str/split character)
      (->> (map #(str/replace % "##" "||")))))
