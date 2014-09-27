(defproject thehat "0.1.0-SNAPSHOT"
  :description "The Hat game for Clojure Cup 2014"
  :url "https://github.com/clojurecup2014/thehat"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/plumbing "0.3.3"]
                 [om "0.7.3"]
                 [prismatic/om-tools "0.3.3" :exclusions [org.clojure/clojure]]
                 [secretary "1.2.0"]
                 [figwheel "0.1.4-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-figwheel "0.1.4-SNAPSHOT"]]

  :source-paths ["src/clj"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs/"]
              :compiler {:output-to "resources/public/js/compiled/thehat.js"
                         :output-dir "resources/public/js/compiled/out"
                         :optimizations :none
                         :source-map true}}
             {:id "release"
              :source-paths ["src/cljs/"]
              :compiler {:output-to "resources/public/thehat.js"
                         :output-dir "resources/public/out_prod"
                         :optimizations :advanced
                         :pretty-print false}}]})
