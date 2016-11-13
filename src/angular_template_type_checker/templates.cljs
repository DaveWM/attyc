(ns angular-template-type-checker.templates
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [angular-template-type-checker.hickory :refer [flatten-hickory get-all-attrs]]
            [angular-template-type-checker.typescript :refer [build-expression-typescript]]))

(defn get-model-info [tags]
  "Gets the metadata for a template. The metadata should be stored in the first comment tag in the template, in the form of a clojure map with keys :model, :type, and :import"
  (let [comment-tag (->> tags
                         (filter #(= :comment (:type %)))
                         first)
        model-info-edn (first (:content comment-tag))]
    (when model-info-edn
      (reader/read-string model-info-edn))))

(defn template-to-typescript [hickory]
  (let [tags (flatten-hickory hickory)
        model-info (get-model-info tags)
        attrs (get-all-attrs tags (filter (fn [[attr value]]
                                            (str/includes? value (:model model-info)))))]
    (->> attrs
         (map (fn [attr]
                (build-expression-typescript attr model-info))))))


