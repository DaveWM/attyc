(defproject angular-template-type-checker "0.1.0-SNAPSHOT"
  :description "Command line app to check the types used in angular templates"
  :url "http://example.com/FIXME"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293"]
                 [davewm/hickory "0.7.1-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.0-2"]
            [lein-npm "0.6.2"]]

  :source-paths ["src"]

  :clean-targets ["server.js"
                  "target"]

  :npm {:dependencies [[ts-node "1.7.0"]
                       [xmldom "0.1.22"]
                       [typescript "2.0.9"]
                       [glob "7.1.1"]
                       [command-line-args "3.0.3"]]
        :devDependencies [[ws "1.1.1"]]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.2"]]
                   :source-paths ["cljs_src" "dev"] }}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  
  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {
                                   :main angular-template-type-checker.core
                                   :output-to "dist/main.js"
                                   :output-dir "target/server_dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {
                                   :main angular-template-type-checker.core
                                   :output-to "dist/main.min.js"
                                   :output-dir "dist"
                                   :target :nodejs
                                   :optimizations :simple
                                   :source-map "dist/main.map.js"}}]})
