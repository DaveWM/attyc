(ns angular-template-type-checker.typescript
  (:require [cljs.nodejs :as node]
            [clojure.string :as str]
            [cljs.spec :as s]
            [angular-template-type-checker.string :refer [split-by-non-repeated]]
            [angular-template-type-checker.specs :refer [metadata-spec]]))

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

(defn build-typescript [metadata expressions global-exprs]
  (let [import-statements (->> (filter :import metadata)
                               (group-by :import)
                               (map (fn [[import metadata]]
                                      (str "import {" (str/join ", " (map :type metadata)) "} from '" import "';"))))
        function-args (->> metadata
                           (map #(str (:name %) ": " (:type %)))
                           (str/join ", "))
        build-function-declaration #(str "let " (gensym "func") " = function (" function-args ")")
        build-function #(str (build-function-declaration) "{ return " % "; };")
        function-statements (map build-function expressions)]
    (->> (concat import-statements
                 [(str (build-function-declaration) "{")]
                 global-exprs
                 function-statements
                 ["}"])
         (apply str))))
(s/fdef build-typescript
        :args (s/alt :metadata metadata-spec
                     :expressions (s/+ string?)
                     :global-expressions (s/* string?))
        :ret string?)
