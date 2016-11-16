(ns angular-template-type-checker.templates
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [angular-template-type-checker.hickory :refer [flatten-hickory get-all-attrs get-all-text-content]]
            [angular-template-type-checker.typescript :refer [build-typescript has-bindings]]))

(defn extract-metadata [tags]
  "Gets the metadata for a template. The metadata should be stored in the first comment tag in the template, in the form of an edn array of maps with keys :name, :type and optionally :import (TODO: write spec for this)"
  (let [comment-tag (->> tags
                         (filter #(= :comment (:type %)))
                         first)
        model-info-edn (->> (:content comment-tag)
                            (filter string?)
                            (apply str))]
    (when model-info-edn
      (reader/read-string model-info-edn))))

(defn extract-expressions [variables tags]
  (let [attrs (->> (get-all-attrs tags)
                   (filter (fn [[attr value]]
                             (->> variables
                                  (some #(str/includes? value %))))))
        bindings (->> (get-all-text-content tags)
                      (filter has-bindings))]
    (concat (map (fn [b] [nil b]) bindings) attrs)))


