(defproject com.github.dgknght/app-lib "0.1.2"
  :description "Library of commonly used functions for web app development"
  :url "https://github.com/dgknght/app-lib"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.758"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/data.zip "0.1.3"]
                 [cheshire "5.8.1"]
                 [crouton "0.1.2"]
                 [clj-time "0.15.2"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [stowaway "0.1.8"]
                 [camel-snake-kebab "0.4.2"]
                 [reagent "0.8.0"]
                 [lein-doo "0.1.11"]
                 [cljsjs/decimal "10.2.0-0"]
                 [lambdaisland/uri "1.4.54"]
                 [cljs-http "0.1.45"]
                 [ring "1.9.0"]]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.11"]]
  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :cljsbuild {:builds [{:source-paths ["src/cljs" "src/cljc"]
                        :compiler {:output-to "target/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}
                       {:id "test"
                        :source-paths ["src/cljs" "src/cljc" "cljs_test"]
                        :compiler {:output-to "out/testable.js"
                                   :main dgknght.app-lib.test-runner
                                   :optimizations :none}}]}
  :doo {:build "test"
        :alias {:default [:firefox]}}
  :prep-tasks ["compile" ["cljsbuild" "once"]]
  :repositories [["clojars" {:creds :gpg}]])
