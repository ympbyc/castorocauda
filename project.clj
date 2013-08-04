(defproject castorocauda "0.0.9"
  :description "Castorocauda is a client side web development framework-ish library that can be mixed freely with other libraries - All castorocauda source included in this repository release under Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)"
  :url "http://changeme.example.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccups "0.2.0"]
                 [prismatic/dommy "0.1.1"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-marginalia "0.7.1"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "resources/public/js/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       {:source-paths ["test"]
                        :compiler {:output-to "resources/public/js/test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
