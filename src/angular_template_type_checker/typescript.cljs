(ns angular-template-type-checker.typescript
  (:require [cljs.nodejs :as node]
            [clojure.string :as str]
            [cljs.spec :as s]
            [angular-template-type-checker.string :refer [split-by-non-repeated]]
            [angular-template-type-checker.specs :refer [metadata-spec]]
            [angular-template-type-checker.hiccup :refer [get-all-tags-of-type flatten-hiccup get-content]]))

(def ts-node (-> (node/require "ts-node")
                 (.register (clj->js {:project "./tsconfig.json"
                                      :compilerOptions {:allowUnreachableCode true}}))))

(def ng-global-vars {"$index" "number"
                     "$first" "boolean"
                     "$middle" "boolean"
                     "$last" "boolean"
                     "$even" "boolean"
                     "$odd" "boolean"})

(defn get-compiler [filename]
  (let [compile (-> (ts-node)
                    (aget "compile"))]
    (fn [code]
      (compile code (str filename ".ts") 0))))

(defn try-compile [code filename]
  "returns nil or an error"
  (try (do ((get-compiler filename) code)
           nil)
       (catch js/Error e (.-message e))))

(defn get-global-ts-expr [hiccup-element]
  (condp = (first hiccup-element)
    :binding-expr (let [[_ & binding-symbols] (first (get-all-tags-of-type :binding-symbols hiccup-element))
                        [_ [_ & first-expr-parts]] (first (get-all-tags-of-type :binding-value hiccup-element)) ; todo treat filters as functions, require types
                        binding-expression (apply str first-expr-parts)]
                    (condp = (count binding-symbols)
                                        ; assume filters don't change the structure of the initial expression result
                      1 (str "let " (first binding-symbols) " = " binding-expression "[0];")
                      2 (str "let " (first binding-symbols) ": string; let " (second binding-symbols) ": any;")
                      nil))
    :ng-expr (str "let " (get-content hiccup-element) ";")
    nil))

(defn build-typescript [metadata angular-expression-trees]
  (let [import-statements (->> (filter :import metadata)
                               (group-by :import)
                               (map (fn [[import metadata]]
                                      (str "import {" (str/join ", " (map :type metadata)) "} from '" import "';"))))
        function-args (->> metadata
                           (map #(str (:name %) ": " (:type %)))
                           (str/join ", "))
        build-function-declaration #(str "let " (gensym "func") " = function (" function-args ")")
        build-function #(str (build-function-declaration) "{ return " % "; };")
        expressions (when (not-empty angular-expression-trees)
                      (->> angular-expression-trees
                           (mapcat #(get-all-tags-of-type :expr %))
                           (map get-content)))
        expression-functions (->> expressions
                                  (map build-function))
        global-exprs (->> angular-expression-trees
                          (mapcat #(get-all-tags-of-type :global %))
                          (mapcat rest)
                          (map get-global-ts-expr)
                          (remove nil?))
        ng-global-var-bindings (->> ng-global-vars
                                    (map (fn [[var type]] (str "let " var ":" type ";")))
                                    (apply str))]
    (->> (concat import-statements
                 [ng-global-var-bindings]
                 [(build-function-declaration) "{"]
                 global-exprs
                 expression-functions
                 ["}"])
         (apply str))))
