(ns angular-template-type-checker.core
  (:require [cljs.nodejs :as node]
            [hickory.core :refer [parse parse-fragment as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [clojure.string :as str]
            [clojure.spec :as s]
            [angular-template-type-checker.hickory :refer [parse-html flatten-hickory]]
            [angular-template-type-checker.typescript :refer [compile build-typescript]]
            [angular-template-type-checker.templates :refer [extract-local-scope-exprs extract-global-scope-exprs extract-metadata]]))

(set! js/DOMParser (.-DOMParser (node/require "xmldom")))
(node/enable-util-print!)

(def glob (node/require "glob"))
(def fs (node/require "fs"))
(def command-line-args (node/require "command-line-args"))

(s/def :metadata/name string?)
(s/def :metadata/type string?)
(s/def :metadata/import string?)
(s/def :metadata/variable (s/keys
                           :req-un [:metadata/name :metadata/type]
                           :opt-un [:metadata/import]))
(def metadata-spec (s/+ :metadata/variable))

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
        typescript (->> tags
                        ((juxt extract-local-scope-exprs extract-global-scope-exprs) (map :name metadata))
                        (apply build-typescript metadata))]
    (if-let [error (check-metadata metadata)]
      error
      (try
        (do (compile typescript filename)
            nil)
        (catch js/Error e (.-message e))))))

(defn verify-templates [glob-pattern]
  (-> (get-file-contents glob-pattern)
      (.then (fn [data]
               (->> data
                    (map (fn [[template-html filename]]
                           [filename (verify-template template-html filename)]))
                    (into {}))))))

(defn process-results [results]
  (let [errored-files (->> results
                           (filter (fn [[_ error]]
                                     error)))]
    (if (not (empty? errored-files))
      (do (println "Some templates failed the type check:\r\n")
          (doseq [[filename error] errored-files]
            (println filename)
            (println "------")
            (println error)
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
    (-> (verify-templates glob)
        (.then process-results)
        (.then #(.exit node/process (if % 0 1)))
        )))

(set! *main-cli-fn* -main)
