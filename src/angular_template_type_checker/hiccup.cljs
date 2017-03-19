(ns angular-template-type-checker.hiccup
  (:require [hickory.zip :refer [hiccup-zip]]
            [clojure.zip :as zip]))


(defn flatten-hiccup [hiccup]
  (->> (loop [zipper (hiccup-zip hiccup)
              results []]
         (if (or (zip/end? zipper))
           results
           (recur (zip/next zipper) (conj results (zip/node zipper)))))
       (remove string?)))

(defn get-all-tags-of-type [tag hiccup]
  (->> (flatten-hiccup hiccup)
       (filter #(= (first %) tag))))

(defn get-content [hiccup]
  (->> (loop [zipper (hiccup-zip hiccup)
              results []]
         (if (or (zip/end? zipper))
           results
           (recur (zip/next zipper) (conj results (zip/node zipper)))))
       (filter string?)
       (apply str)))
