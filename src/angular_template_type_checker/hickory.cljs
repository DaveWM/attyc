(ns angular-template-type-checker.hickory
  (:require [hickory.core :refer [parse as-hickory]]
            [hickory.zip :refer [hickory-zip]]))

(defn flatten-hickory [hickory]
  "flattens a hickory structure, and returns a seq of all tags"
  (->> hickory
       (tree-seq #(sequential? (:content %)) :content)))

(defn parse-html [html]
  (->> html
       parse
       as-hickory))

(defn get-all-attrs [tags transducer]
  (->> tags
       (mapcat :attrs)
       (into [] transducer)
       (map last)))
