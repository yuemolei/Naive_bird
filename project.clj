(defproject project "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [ring "1.6.3"]
                 [metosin/ring-http-response "0.9.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.0"]
                 [ring-webjars "0.2.0"]
                 [org.webjars/jquery "3.3.1-1"]
                 [org.webjars/bootstrap "4.0.0-2"]
                 [org.webjars/popper.js "1.14.1"]
                 [org.webjars/font-awesome "5.2.0"]
                 [domina "1.0.3"]
                 [reagent "0.9.0-rc2"]
                 [reagent-utils "0.3.1"]
                 [cljs-ajax "0.7.5"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "42.2.4"]
                 [ragtime "0.7.2"]
                 [buddy "2.0.0"]
                 [clj-time "0.14.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.12"]
                 [bouncer "1.0.1"]
                 [org.clojure/core.async "0.3.442"]
                 [mysql/mysql-connector-java "8.0.18"]
                 [cljs-ajax "0.8.0"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [org.clojure/data.json "0.2.7"]]
  :main ^:skip-aot project.core
  :plugins [[lein-ring "0.12.5"]
            [lein-cljsbuild "1.1.7" :excludes [[org.clojure/clojure]]]
            [lein-figwheel "0.5.19"]]
  :ring {:handler project.core/app}
  :source-paths ["src"]
  :resource-paths ["resources"]

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :dev :compiler :output-dir]
                                    [:cljsbuild :builds :dev :compiler :output-to]]
  :cljsbuild
  {:builds {:dev
            {:source-paths ["src-cljs"]
             :figwheel     false
             :compiler     {:main                 project.core
                            :asset-path           "js/out"
                            :output-to            "resources/public/js/main.js"
                            :output-dir           "resources/public/js/out"
                            :optimizations :none
                            :source-map-timestamp true
                            :pretty-print         true}}}}
  :figwheel {:css-dirs ["resources/public/css"]
             :open-file-command "emacsclient"})
