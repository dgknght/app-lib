(defproject com.github.dgknght/app-lib "0.3.11"
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
                 [stowaway "0.1.10"]
                 [camel-snake-kebab "0.4.2"]
                 [reagent "0.8.0"]
                 [lein-doo "0.1.11"]
                 [cljsjs/decimal "10.2.0-0"]
                 [lambdaisland/uri "1.4.54"]
                 [clj-http "3.12.3"]
                 [cljs-http "0.1.46"]
                 [ring "1.9.0"]]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.11"]
            [lein-cloverage "1.2.4"]]
  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :clean-targets [:target-path :compile-path "out"]
  :cljsbuild {:builds [{:source-paths ["src/cljs" "src/cljc"]
                        :compiler {:output-to "target/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}
                       {:id "test"
                        :source-paths ["src/cljs" "src/cljc" "test"]
                        :compiler {:output-to "out/testable.js"
                                   :output-dir "out"
                                   :target :nodejs
                                   :main dgknght.app-lib.test-runner
                                   :optimizations :none}}]}
  :doo {:build "test"
        :alias {:default [:node]}}
  :cloverage {:fail-threshold 90
              :high-watermark 90
              :ns-exclude-regex [#"dgknght.app-lib.client-macros"
                                 #"dgknght.app-lib.forms-validation"
                                 #"dgknght.app-lib.json-encoding"
                                 #"dgknght.app-lib.test-assertions.cljs"
                                 #"dgknght.app-lib.test-cljs"
                                 #"dgknght.app-lib.web-mocks.cljs"]}
  :prep-tasks ["compile" ["cljsbuild" "once"]]
  :jvm-opts  ["-Duser.timezone=UTC"
              "-Duser.country=US"
              "-Duser.language=en"]
  :repositories [["clojars" {:creds :gpg}]])
