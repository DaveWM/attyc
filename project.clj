(defproject angular-template-type-checker "0.1.0-SNAPSHOT"
  :description "Command line app to check the types used in angular templates"
  :url "http://example.com/FIXME"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293"]
                 [davewm/hickory "0.7.1-SNAPSHOT"]
                 [org.clojure/test.check "0.9.0"] ; needed due to this bug: http://dev.clojure.org/jira/browse/CLJS-1792?page=com.atlassian.jira.plugin.system.issuetabpanels:changehistory-tabpanel
                 [instaparse "1.4.5"]
                 ]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.0-2"]]
            

  :source-paths ["src"]

  :clean-targets ["server.js"
                  "target"]

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
