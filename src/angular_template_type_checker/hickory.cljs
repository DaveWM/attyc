(ns angular-template-type-checker.hickory
  (:require [hickory.core :refer [parse as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [hickory.select :as s]))

(defn flatten-hickory [hickory]
  "flattens a hickory structure, and returns a seq of all tags"
  (tree-seq :content :content hickory))

(defn parse-html [html]
  (->> html
       parse
       as-hickory))

(defn get-all-attrs [tags]
  (mapcat :attrs tags))

(defn get-all-text-content [tags]
  (filter string? tags))

(defn get-all-tags-of-type [tag hickory]
  (s/select (s/tag tag) hickory))
