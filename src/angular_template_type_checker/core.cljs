(ns angular-template-type-checker.core
  (:require [cljs.nodejs :as node]
            [hickory.core :refer [parse parse-fragment as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [clojure.string :as str]
            [cljs.reader :as reader]))

(set! js/DOMParser (.-DOMParser (node/require "xmldom")))
(node/enable-util-print!)

(def ts-node (-> (node/require "ts-node")
                 (.register (clj->js {:project "./tsconfig.json"}))))
(def glob (node/require "glob"))
(def fs (node/require "fs"))
(def command-line-args (node/require "command-line-args"))

(defn get-compiler [filename]
  (let [compile (-> (ts-node)
                    (aget "compile"))]
    (fn [code]
      (compile code (str filename ".ts") 0))))


(def test-html "
  <!-- {:model \"model\" :type \"ModelType\" :import \"./main\"} -->
  <p ng-model='model.x'>hello 
  <span y='model.y'>world</span>
  </p>")
(def test-hickory (->> (parse test-html)
                       as-hickory))


(defn get-model-info [tags]
  "Gets the metadata for a template. The metadata should be stored in the first comment tag in the template, in the form of a clojure map with keys :model, :type, and :import"
  (let [comment-tag (->> tags
                         (filter #(= :comment (:type %)))
                         first)]
    (reader/read-string (first (:content comment-tag)))))

(defn flatten-hickory [hickory]
  "flattens a hickory structure, and returns a seq of all tags"
  (->> hickory
       (tree-seq #(sequential? (:content %)) :content)))

(defn get-attrs-for-model [tags model-info]
  (->> tags
       (mapcat :attrs)
       (filter (fn [[attr value]]
                 (str/includes? value (:model model-info))))
       (map last)))

(defn build-expression-typescript [expression {:keys [model type import]}]
  (let [import-statement (when import (str "import {" type "} from '" import "';"))
        function-str (str "function f (" model ": " type "){ " expression "; }")]
    (str import-statement function-str)))

(defn template-to-typescript [html]
  (let [tags (->> (parse html)
                  as-hickory
                  flatten-hickory)
        model-info (get-model-info tags)
        attrs (get-attrs-for-model tags model-info)]
    (->> attrs
         (map (fn [attr]
                (build-expression-typescript attr model-info))))))

(defn get-file-contents [glob-pattern]
  (-> (js/Promise. (fn [res rej]
                     (glob glob-pattern (fn [err files]
                                          (if err (rej) (res files))
                                          (doseq [file files]                         
                                            )))))
      (.then (fn [files]
               (->> files
                    (map #(js/Promise. (fn [res rej]
                                         (.readFile fs % "utf8" (fn [err contents]
                                                                  (if err (rej) (res [contents %])))))))
                    (.all js/Promise))))))

(defn verify-template [html filename]
  (->> html
       template-to-typescript
       (map (fn [ts-expr]
              (try
                (do ((get-compiler filename) ts-expr)
                    nil)
                (catch js/Error e e))))))

(defn verify-templates [glob-pattern]
  (-> (get-file-contents "**/*.tpl.html")
      (.then (fn [data]
               (->> data
                    (map (fn [[template-html filename]]
                           [filename (verify-template template-html filename)]))
                    (into {}))))))

(defn process-results [results]
  (let [errored-files (->> results
                           (map (fn [[k results]]
                                  [k (filter identity results)]))
                           (filter (fn [[_ errors]]
                                     (not (empty? errors)))))]
    (if (not (empty? errored-files))
      (do (println "Some templates failed the type check:\r\n")
          (doseq [[filename errors] errored-files]
            (println filename)
            (println "------")
            (doseq [error (filter identity errors)]
              (println error))
            (println))
          false)
      (do (println (str (count results) " files verified"))
          true))))

(def cli-option-defs
  (clj->js [{:name "glob"
             :alias "g"
             :type js/String
             :defaultOption true}]))

(defn -main []
  (let [{:keys [glob] :as options} (js->clj (command-line-args cli-option-defs) :keywordize-keys true)]
    (-> (verify-templates "**/*.tpl.html")
        (.then process-results)
        (.then #(.exit node/process (if % 0 1))))))

(set! *main-cli-fn* -main)
