(ns angular-template-type-checker.dev
  (:require [cljs.nodejs :as node]))

(node/enable-util-print!)

(defn -main []
  (println "Started"))

(set! *main-cli-fn* -main)
