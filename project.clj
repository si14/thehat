(defproject thehat "0.1.0-SNAPSHOT"
  :description "The Hat game for Clojure Cup 2014"
  :url "https://github.com/clojurecup2014/thehat"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]
                 [prismatic/om-tools "0.3.3" :exclusions [org.clojure/clojure]]
                 [secretary "1.2.0"]
                 [prismatic/dommy "0.1.3"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src/clj"]

  :profiles {:dev {:dependencies [[figwheel "0.1.4-SNAPSHOT"]
                                  [aysylu/loom "0.5.0"]
                                  [org.clojure/data.json "0.2.5"]
                                  [edu.stanford.nlp/stanford-corenlp "3.3.1"]
                                  [edu.stanford.nlp/stanford-corenlp "3.3.1"
                                   :classifier "models"]]
                   :figwheel {:css-dirs ["resources/public/css"]}
                   :plugins [[lein-figwheel "0.1.4-SNAPSHOT"]]}}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs/" "src-dev/cljs/"]
              :compiler {:output-to "resources/public/js/compiled/thehat.js"
                         :output-dir "resources/public/js/compiled/out"
                         :optimizations :none
                         :source-map true}}
             {:id "release"
              :source-paths ["src/cljs/"]
              :compiler {:output-to "resources/public/thehat.js"
                         :output-dir "resources/public/out_prod"
                         :optimizations :advanced
                         :pretty-print false
                         :preamble ["react/react.min.js"]
                         :externs ["react/externs/react.js"]}}]})
