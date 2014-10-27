(defproject thehat "0.1.0-SNAPSHOT"
  :description "The Hat game for Clojure Cup 2014"
  :url "https://github.com/clojurecup2014/thehat"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.4.2"]
                 [prismatic/schema "0.3.1"]
                 [prismatic/dommy "1.0.0"]
                 [prismatic/plumbing "0.3.5"]
                 [garden "1.2.5"] ;; ?
                 ]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-asset-minifier "0.2.0"]]

  :source-paths ["src/clj"]

  :profiles {:dev {:dependencies [[figwheel "0.1.4-SNAPSHOT"]]
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
                         :preamble ["public/js/geopattern.min.js"
                                    ;; "public/js/facebook.js"
                                    "public/js/twitter.js"
                                    ;; "public/js/googleplus.js"
                                    "react/react_with_addons.min.js"]
                         :externs ["externs.js"
                                   "react/externs/react_with_addons.js"]}}]}

  :minify-assets {:assets {"resources/public/thehat.css"
                           "resources/public/css/"}}
  )
