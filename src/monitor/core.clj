(ns monitor.core
  (:refer-clojure :exclude [extend])
  (:require [clojure.java.jmx :as jmx])
  (:use clj-time.core
        monitor.event)
  (:import (javax.management.remote JMXConnector))
  (:gen-class))

(defn find-mbeans [query & {:keys [hostname port jndi-path username password]
                            :or {hostname "ewaf-test.colo.elex.be"
                                 port 1099
                                 jndi-path "karaf-root"
                                 username "smx"
                                 password "smx"}}]
  (jmx/with-connection {:host hostname
                        :port port
                        :jndi-path jndi-path
                        :environment {JMXConnector/CREDENTIALS (into-array String [username password])}}
    (let [mb (jmx/mbean-names query)
          uris (map #(.getCanonicalName %) mb)]
      (doall (map (fn [uri] {:uri uri
                             :mbean (jmx/mbean uri)}) uris)))))


(defn monitor-objects
  "Monitor all JMX MBeans that correspond to a given query.  Send the results to an agent."
  [query & {:keys [hostname port jndi-path username password]
            :or {hostname "ewaf-test.colo.elex.be"
                 port 1099
                 jndi-path "karaf-root"
                 username "smx"
                 password "smx"}}]
  (while true
   (let [ts (.toString (now))
         mb (find-mbeans query :hostname hostname :port port :jndi-path jndi-path :username username :password password)]
     (println "Logging events for host" hostname)
     (doall (map (fn [m] (save! hostname ts (:uri m) (:mbean m))) mb)))
   (Thread/sleep 60000)))

(defn start!
  []
  (init-db)

  (for [host '("ewaf-test.colo.elex.be" "ewaf-uat.colo.elex.be" "ewaf.colo.elex.be")]
    (.start
     (Thread. (fn [] (monitor-objects "org.apache.activemq:*,Type=Queue" :hostname host))))))