(ns user
  (:use [figwheel-sidecar.repl-api :as ra]))

(defn fig-start! [] (ra/start-figwheel!))

(defn fig-stop! [] (ra/stop-figwheel!))

(defn start! [] (do (fig-start!)
                    (cljs-repl)))
