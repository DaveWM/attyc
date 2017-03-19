(ns angular-template-type-checker.core
  (:require [cljs.nodejs :as node]
            [hickory.core :refer [parse parse-fragment as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [clojure.string :as str]
            [cljs.spec :as s]
            [angular-template-type-checker.hickory :refer [parse-html flatten-hickory]]
            [angular-template-type-checker.typescript :refer [try-compile build-typescript]]
            [angular-template-type-checker.templates :refer [extract-expressions extract-metadata]]
            [angular-template-type-checker.specs :refer [metadata-spec]]
            [instaparse.core :as insta])
  (:require-macros [angular-template-type-checker.macros :refer [def-cli-opts]]))

(set! js/DOMParser (.-DOMParser (node/require "xmldom")))
(node/enable-util-print!)
(def glob (node/require "glob"))
(def fs (node/require "fs"))
(def command-line-args (node/require "command-line-args"))
(def getUsage (node/require "command-line-usage"))

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

(defn check-metadata [metadata]
  "Checks whether the given metadata is valid. Returns nil if it is, or an error string if not"
  (if (nil? metadata)
    "Could not find metadata"
    (when-let [spec-error (s/explain-data metadata-spec metadata)]
      (str "Error with metadata: " (:cljs.spec/problems spec-error)))))

(defn verify-template [html filename]
  (println "verifying " filename)
  (let [tags (->> html
                  parse-html
                  flatten-hickory)
        metadata (extract-metadata tags)
        var-names (map :name metadata)]
    (if-let [error (check-metadata metadata)]
      [error]
      (let [[correct-exprs incorrect-exprs] (->> tags
                                                 (mapcat #(extract-expressions % var-names))
                                                 ((juxt remove filter) insta/failure?))
            typescript (build-typescript metadata correct-exprs)
            compilation-error (try-compile typescript filename)]
        (->> (conj incorrect-exprs
                   compilation-error)
             (remove nil?))))))

(defn verify-templates [glob-pattern]
  (-> (get-file-contents glob-pattern)
      (.then (fn [data]
               (->> data
                    (map (fn [[template-html filename]]
                           [filename (verify-template template-html filename)]))
                    (into {}))))))

(defn process-results [results]
  (let [errored-files (->> results
                           (remove (fn [[_ errors]]
                                     (empty? errors))))]
    (if (not (empty? errored-files))
      (do (println "Some templates failed the type check:\r\n")
          (doseq [[filename errors] errored-files]
            (println filename)
            (println "------")
            (doall (map println errors))
            (println))
          false)
      (do (println (str (count results) " files verified"))
          true))))

(def-cli-opts cli-option-defs
  [{:name "glob"
    :alias "g"
    :type js/String
    :defaultOption true
    :description "Check templates matching this glob"
    :typeLabel "[underline]{glob}"}
   {:name "help"
    :alias "h"
    :type js/Boolean
    :description "Shows a help message"}])

(defn -main []
  (let [{:keys [glob help]} (js->clj (command-line-args (clj->js cli-option-defs)) :keywordize-keys true)]
    (if help
      (print (getUsage (clj->js [{:header "ATTyC"
                                  :content "A command line tool for checking typed angularjs templates"}
                                 {:header "Options"
                                  :optionList cli-option-defs}])))
      (-> (verify-templates glob)
          (.then process-results)
          (.then #(.exit node/process (if % 0 1)))))))

(set! *main-cli-fn* -main)
