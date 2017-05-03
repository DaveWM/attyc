(ns angular-template-type-checker.templates
  (:require [cljs.reader :as reader]
            [clojure.string :as cstr]
            [angular-template-type-checker.hickory :refer [flatten-hickory get-all-attrs get-all-text-content]]
            [angular-template-type-checker.hiccup :refer [get-all-tags-of-type]]
            [angular-template-type-checker.typescript :refer [build-typescript]]
            [angular-template-type-checker.string :refer [split-by-non-repeated]]
            [angular-template-type-checker.parsers :refer [template-expression-parser ng-repeat-parser ng-options-parser single-expression-parser]]
            [instaparse.core :as insta]))

(def ng-global-attrs #{:ng-repeat :ng-init :ng-options})
(def ng-attrs #{:ng-app :ng-init :ng-bind :ng-non-bindable :ng-bind-template :ng-bind-html :ng-change :ng-checked :ng-class :ng-cloak :ng-click :ng-controller :ng-disabled :ng-form :ng-include :ng-if :ng-switch :ng-model :ng-readonly :ng-selected :ng-show :ng-submit :ng-value :ng-required :ng-style :ng-pattern :ng-maxlength :ng-minlength :ng-classeven :ng-classodd :ng-cut :ng-copy :ng-paste :ng-open})

(def ng-attr-parsers (merge  {:ng-repeat ng-repeat-parser
                              :ng-options ng-options-parser}
                             (zipmap ng-attrs
                                     (repeat single-expression-parser))))

(defn extract-metadata [tags]
  "Gets the metadata for a template. The metadata should be stored in the first comment tag in the template"
  (let [first-tag (->> tags
                       (filter #(#{:element :comment} (:type %)))
                       first)
        model-info-edn (when (= :comment (:type first-tag))
                         (->> (:content first-tag)
                              (filter string?)
                              (apply str)))]
    (when model-info-edn
      (reader/read-string model-info-edn))))

(defn extract-curly-brace-expressions [tag]
  (->> (:content tag)
       (concat (vals (:attrs tag)))
       (filter string?)
       (map template-expression-parser)
       (remove insta/failure?)))

(defn extract-ng-attr-expressions [tag]
  (->> (:attrs tag)
       (map (fn [[k v]]
              (when-let [parser (k ng-attr-parsers)]
                (if (ng-global-attrs k)
                  [:global (parser v)]
                  (parser v)))))
       (remove nil?)))

(defn extract-attr-expressions-for-variable [tag var-name]
  (->> (:attrs tag)
       vals
       (map (fn [attr]
              (let [parsed (single-expression-parser attr :unhide :tags)]
                (when (and (not (insta/failure? parsed))
                           (->> parsed
                                (get-all-tags-of-type :symbol)
                                (mapcat :content)
                                (some #(= % var-name))))
                  (single-expression-parser attr)))))
       (remove nil?)))

(defn extract-expressions [tag var-names]
  (concat (extract-curly-brace-expressions tag)
          (extract-ng-attr-expressions tag)
          (mapcat (partial extract-attr-expressions-for-variable tag) var-names)))


