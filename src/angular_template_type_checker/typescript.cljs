(ns angular-template-type-checker.typescript
  (:require [cljs.nodejs :as node]
            [clojure.string :as str]
            [angular-template-type-checker.string :refer [split-by-non-repeated]]))

(def ts-node (-> (node/require "ts-node")
                 (.register (clj->js {:project "./tsconfig.json"
                                      :compilerOptions {:allowUnreachableCode true}}))))

(defn get-compiler [filename]
  (let [compile (-> (ts-node)
                    (aget "compile"))]
    (fn [code]
      (compile code (str filename ".ts") 0))))

(defn compile [code filename]
  ((get-compiler filename) code))

(defn has-bindings [text]
  (str/includes? text "{{"))

(defn extract-bindings [text]
  "gets a sequence of bindings (bits in curly braces) from some text, or returns nil if there aren't any"
  (let [curly-brace-regex #"\{\{\s*(.*?)\s*\}\}"]
    (->> (re-seq curly-brace-regex text)
         (mapcat rest)
         not-empty)))

(defn get-exprs-for-attr [[attr value]]
  (->> (case attr
         :ng-repeat (let [regex #"\S+\s+in\s+(.*)"]
                      (rest (re-find regex value))) ; remove the "x in" bit from the ng-repeat
         (or (extract-bindings value) [value]))
       (mapcat (fn [ng-expr]
                 (let [[expr & filters] (split-by-non-repeated ng-expr \|)]
                   (cons expr
                         (mapcat #(rest (str/split % ":")) filters)))))))

(defn get-global-scope-exprs [[attr value]]
  "get all expressions that should have global scope, e.g. for variable assignment"
  (case attr
    :ng-repeat (let [regex #"(\S+)\s+in\s+([^\s\|]+)"
                     [_ variable array] (re-find regex value)]
                 (str "let " variable " = " array "[0];"))
    :ng-init value
    nil))

(defn build-typescript [attributes metadata]
  (let [import-statements (->> (filter :import metadata)
                               (group-by :import)
                               (map (fn [[import metadata]]
                                      (str "import {" (str/join ", " (map :type metadata)) "} from '" import "';"))))
        global-statements (->> attributes
                               (map get-global-scope-exprs)
                               (filter identity))
        function-args (->> metadata
                           (map #(str (:name %) ": " (:type %)))
                           (str/join ", "))
        build-function-declaration #(str "let " (gensym "func") " = function (" function-args ")")
        build-function #(str (build-function-declaration) "{ return " % "; };")
        function-statements (->> attributes
                                 (mapcat get-exprs-for-attr)
                                 (map build-function))]
    (->> (concat import-statements
                 [(str (build-function-declaration) "{")]
                 global-statements
                 function-statements
                 ["}"])
         (apply str))))
