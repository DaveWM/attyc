(ns angular-template-type-checker.templates
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [angular-template-type-checker.hickory :refer [flatten-hickory get-all-attrs get-all-text-content]]
            [angular-template-type-checker.typescript :refer [build-typescript]]
            [angular-template-type-checker.string :refer [split-by-non-repeated]]))

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

(defn extract-bindings [text]
  "gets a sequence of bindings (bits in curly braces) from some text, or returns nil if there aren't any"
  (let [curly-brace-regex #"\{\{\s*(.*?)\s*\}\}"]
    (->> (re-seq curly-brace-regex text)
         (mapcat rest))))

(defn extract-filter-expressions [ng-expr]
  (let [[expr & filters] (split-by-non-repeated ng-expr \|)]
    (cons expr
          (mapcat #(rest (str/split % ":")) filters))))

(defn get-attr-global-scope-expr [[attr value]]
  (case attr
    :ng-repeat (let [regex #"(\S+)\s+in\s+([^\s\|]+)"
                     [_ variable array] (re-find regex value)]
                 (str "let " variable " = " array "[0];"))
    :ng-init value
    nil))

(defn extract-global-scope-exprs [variables tags]
  "get all expressions that should have global scope, e.g. for variable assignment in ng-init"
  (->> (get-all-attrs tags)
       (map get-attr-global-scope-expr)
       (filter some?)))

(defn get-binding-exprs-for-attr [[attr value]]
  (->> (case attr
         :ng-repeat (let [regex #"\S+\s+in\s+(.*)"]
                      (rest (re-find regex value))) ; remove the "x in" bit from the ng-repeat
         (or (extract-bindings value) [value]))))

(defn extract-local-scope-exprs [variables tags]
  "Extract all expressions that are assumed to have their own scope (i.e. variable declarations don't affect other expressions)"
  (let [attr-expressions (->> (get-all-attrs tags)
                              (filter (fn [[attr value]]
                                        (->> variables
                                             (some #(str/includes? value %)))))
                              (mapcat (fn [[attr value :as attribute]]
                                        (let [bindings (get-binding-exprs-for-attr attribute)]
                                          (if (not-empty bindings)
                                            bindings
                                            [value])))))
        bindings-expressions (->> (get-all-text-content tags)
                                  (mapcat extract-bindings))]
    (->> (concat attr-expressions bindings-expressions)
         (mapcat extract-filter-expressions))))


