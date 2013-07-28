(defproject castorocauda "0.0.1"
  :description "Castorocauda is a clone of WebFUI with radical changes in its internal - All castorocauda source included in this repository release under Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)"
  :url "http://changeme.example.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.monads "0.1.4"]
                 [hiccups "0.2.0"]]
  :dev-dependencies [[lein-autodoc "0.9.0"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.7.1"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "resources/public/js/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
