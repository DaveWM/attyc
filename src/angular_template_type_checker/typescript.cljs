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

(defn get-exprs-for-attr [[attr value]]
  (let [ng-repeat-regex #"\S+\s+in\s+(\S+)(?:\s+track\s+by\s+\S+)?"]
    (case attr
      :ng-repeat (next (re-find ng-repeat-regex value))
      [value])))

(defn build-typescript [attributes {:keys [model type import]}]
  (let [import-statement (when import (str "import {" type "} from '" import "';"))
        build-function #(str "function " (gensym "func") " (" model ": " type "){ " % "; }")]
    (apply str
           import-statement
           (->> attributes
                (mapcat get-exprs-for-attr)
                (map build-function)))))
