(ns angular-template-type-checker.core
  (:require [cljs.nodejs :as node]
            [hickory.core :refer [parse parse-fragment as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [clojure.string :as str]
            [cljs.spec :as s]
            [angular-template-type-checker.hickory :refer [parse-html flatten-hickory]]
            [angular-template-type-checker.typescript :refer [try-compile build-typescript get-compiler]]
            [angular-template-type-checker.templates :refer [extract-expressions extract-metadata]]
            [angular-template-type-checker.specs :refer [metadata-spec]]
            [angular-template-type-checker.logging :refer [debug info error]]
            [instaparse.core :as insta])
  (:require-macros [angular-template-type-checker.macros :refer [def-cli-opts with-log-level]]))

(set! js/DOMParser (.-DOMParser (node/require "xmldom")))
(node/enable-util-print!)
(def glob (node/require "glob"))
(def fs (node/require "fs"))
(def command-line-args (node/require "command-line-args"))
(def getUsage (node/require "command-line-usage"))
(def known-errors-map {:no-metadata "Could not find metadata"})

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
    :no-metadata
    (when-let [spec-error (s/explain-data metadata-spec metadata)]
      (str "Error with metadata: " (:cljs.spec/problems spec-error)))))

(defn verify-template [html filename & [ts-config-path]]
  (info "verifying " filename)
  (let [tags (->> html
                  parse-html
                  flatten-hickory
                  (filter map?)
                  (map #(update % :attrs (comp (partial into {})
                                               (partial map (fn [[k v]]
                                                              [k (str/replace v #"\n" "")]))))))
        metadata (extract-metadata tags)
        var-names (map :name metadata)]
    (if-let [error (check-metadata metadata)]
      [error]
      (let [[correct-exprs incorrect-exprs] (->> tags
                                                 (mapcat #(extract-expressions % var-names))
                                                 ((juxt remove filter) insta/failure?))
            typescript (build-typescript metadata correct-exprs)
            compiler (get-compiler filename (when ts-config-path {:project ts-config-path}))
            compilation-error (try-compile typescript compiler)]
        (debug "generated typescript:")
        (debug typescript)
        (->> (conj incorrect-exprs
                   compilation-error)
             (remove nil?))))))

(defn verify-templates [glob-pattern & [ts-config-path]]
  (-> (get-file-contents glob-pattern)
      (.then (fn [data]
               (->> data
                    (map (fn [[template-html filename]]
                           [filename (verify-template template-html filename ts-config-path)]))
                    (into {}))))))

(defn format-error-messages [errors-map results]
  (map (fn [[filename errors]]
         [filename (map #(or (get errors-map %) %) errors)])
       results))

(defn process-results [results]
  (let [errored-files (->> results
                           (remove (fn [[_ errors]]
                                     (empty? errors))))]
    (if (not (empty? errored-files))
      (do (info "Some templates failed the type check:\r\n")
          (doseq [[filename errors] errored-files]
            (error filename)
            (error "------")
            (doall (map error errors))
            (error ""))
          false)
      (do (info (str (count results) " files verified"))
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
    :description "Shows a help message"}
   {:name "ignore-no-metadata"
    :alias "m"
    :type js/Boolean
    :description "Ignore files with no metadata"}
   {:name "verbose"
    :alias "v"
    :type js/Boolean
    :description "Enable verbose logging. Will log generated typescript."}
   {:name "ts-config-path"
    :alias "c"
    :type js/String
    :description "The relative path to your tsconfig file. Defaults to './tsconfig.json'."
    :typeLabel "[underline]{path}"}])

(defn -main []
  (let [{:keys [glob help ignore-no-metadata verbose ts-config-path]} (js->clj (command-line-args (clj->js cli-option-defs)) :keywordize-keys true)]
    (if help
      (print (getUsage (clj->js [{:header "ATTyC"
                                  :content "A command line tool for checking typed angularjs templates"}
                                 {:header "Options"
                                  :optionList cli-option-defs}])))
      (-> (get-file-contents glob)
          (.then (fn [data]
                   (with-log-level (if verbose :debug :info)
                     (let [results (->> data
                                        (map (fn [[template-html filename]]
                                               [filename (verify-template template-html filename ts-config-path)]))
                                        (map (fn [[filename errors]]
                                               [filename (if ignore-no-metadata
                                                           errors
                                                           (remove #(= % :no-metadata) errors))]))
                                        (format-error-messages known-errors-map)
                                        (process-results))]))))
          (.then #(.exit node/process (if % 0 1)))
          (.catch #(error (.-message %)))))))

(set! *main-cli-fn* -main)
