(ns angular-template-type-checker.macros
  (:require [clojure.spec :as s]
            [angular-template-type-checker.specs :as specs :refer [optionsListSpec]]))

(defmacro def-cli-opts [var-name options]
  (if-let [explanation (s/explain-data optionsListSpec options)]
    (throw (Exception. (str "Invalid command line arguments: " explanation)))
    `(def ~var-name ~options)))
