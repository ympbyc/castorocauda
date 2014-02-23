(defproject castorocauda "0.1.2"
  :description "Castorocauda provides an abstraction layer on top of the DOM"
  :url "https://github.com/ympbyc/castorocauda"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccups "0.2.0"]
                 [prismatic/dommy "0.1.1"]

                 ;;uncomment the following line when building tests
                 ;;[org.clojure/core.async "0.1.0-SNAPSHOT"]
                 ]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-marginalia "0.7.1"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "resources/public/js/main.js"
                                   :optimizations :simple
                                   :pretty-print true}}

                       ;;uncomment following lines when building tests
                       ;;{:source-paths ["test"]
                       ;; :compiler {:output-to "resources/public/js/test.js"
                       ;;            :optimizations :simple
                       ;;            :pretty-print true}}
                       ]})
