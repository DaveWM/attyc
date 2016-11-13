(ns angular-template-type-checker.core
  (:require [cljs.nodejs :as node]
            [hickory.core :refer [parse parse-fragment as-hickory]]
            [hickory.zip :refer [hickory-zip]]
            [clojure.string :as str]
            [angular-template-type-checker.hickory :refer [parse-html flatten-hickory]]
            [angular-template-type-checker.typescript :refer [compile]]
            [angular-template-type-checker.templates :refer [template-to-typescript]]))

(set! js/DOMParser (.-DOMParser (node/require "xmldom")))
(node/enable-util-print!)

(def glob (node/require "glob"))
(def fs (node/require "fs"))
(def command-line-args (node/require "command-line-args"))

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
  (println "verifying " filename)
  (->> html
       parse-html
       template-to-typescript
       (map (fn [ts-expr]
              (try
                (do (compile ts-expr filename)
                    nil)
                (catch js/Error e e))))))

(defn verify-templates [glob-pattern]
  (-> (get-file-contents glob-pattern)
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
    (-> (verify-templates glob)
        (.then process-results)
        ;(.then #(.exit node/process (if % 0 1)))
        )))

(set! *main-cli-fn* -main)
