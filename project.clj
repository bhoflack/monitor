(defproject monitor "1.0.0-SNAPSHOT"
  :description "Monitor project for electronic wafermapping"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jmx "0.2.0"]
                 [clj-time "0.3.3"]
                 [hsqldb/hsqldb "1.8.0.10"]
                 [org.clojure/java.jdbc "0.2.0"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.0"]
                 [ring/ring-jetty-adapter "1.1.0"]]
  :dev-dependencies [[swank-clojure "1.3.4"]
                     [lein-ring "0.7.0"]]
  :main monitor.core
  :ring {:handler monitor.web/app})