(ns angular-template-type-checker.hickory
  (:require [hickory.core :refer [parse as-hickory]]
            [hickory.zip :refer [hickory-zip]]))

(defn flatten-hickory [hickory]
  "flattens a hickory structure, and returns a seq of all tags"
  (->> hickory
       (tree-seq #((every-pred sequential? (complement string?)) (:content %)) :content)))

(defn parse-html [html]
  (->> html
       parse
       as-hickory))

(defn get-all-attrs [tags]
  (->> tags
       (mapcat :attrs)))

(defn get-all-text-content [tags]
  (->> tags
       (filter #(= :element (:type %)))
       (mapcat :content)
       (filter string?)))
