(ns angular-template-type-checker.typescript
  (:require [cljs.nodejs :as node]))

(def ts-node (-> (node/require "ts-node")
                 (.register (clj->js {:project "./tsconfig.json"}))))

(defn get-compiler [filename]
  (let [compile (-> (ts-node)
                    (aget "compile"))]
    (fn [code]
      (compile code (str filename ".ts") 0))))

(defn compile [code filename]
  ((get-compiler filename) code))

(defn build-expression-typescript [expression {:keys [model type import]}]
  (let [import-statement (when import (str "import {" type "} from '" import "';"))
        function-str (str "function f (" model ": " type "){ " expression "; }")]
    (str import-statement function-str)))
