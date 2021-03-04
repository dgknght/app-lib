(defproject dgknght/app-lib "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.758"]]
  ; TODO: add resource-paths, in particular, reference the output of cljs build
  :repl-options {:init-ns dgknght.app-lib}
  :plugins [[lein-cljsbuild "1.1.8"]]
  :cljsbuild {:builds [{:source-paths ["src/dgknght"]
                        :compiler {:output-to "target/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}]}
  :hooks [leiningen.cljsbuild])
