(ns monitor.core
  (:refer-clojure :exclude [extend])
  (:require [clojure.java.jmx :as jmx])
  (:use clj-time.core)
  (:import (javax.management.remote JMXConnector)))

(def mbeans (agent {}))

(defn find-all-with-query
  [query]
  (let [objs (jmx/mbean-names query)
        uris (map #(.getCanonicalName %) objs)]
    (map (fn [uri] {:uri uri :mbean jmx/mbean}) uris)))

(defn log-event
  "Log an event"
  [data hostname timestamp name value]
  (let [timestamps (get data hostname {})
        values (get timestamps timestamp {})]
    (println "Logging " hostname " " timestamp " " name " " value)
    (assoc data hostname
           (assoc timestamps timestamp
                  (assoc values name value)))))

(defn monitor-objects
  "Monitor all JMX MBeans that correspond to a given query.  Send the results to an agent."
  [query mbeans & {:keys [hostname port jndi-path username password]
                  :or {hostname "ewaf-test.colo.elex.be"
                       port 1099
                       jndi-path "karaf-root"
                       username "smx"
                       password "smx"}}]

  (jmx/with-connection {:host hostname
                        :port port
                        :jndi-path jndi-path
                        :environment {JMXConnector/CREDENTIALS (into-array String [username password])}}
    (loop []
      (let [r (doall (find-all-with-query query))
            ts (.toString (now))]
        (doseq [q r]
          (send mbeans log-event hostname ts (:uri q) (:mbean q)))
        (Thread/sleep 5000)
        (recur)))))

(defn start!
  []
  (for [host '("ewaf-test.colo.elex.be" "ewaf-uat.colo.elex.be" "ewaf.colo.elex.be")]
    (.start
     (Thread. (fn [] (monitor-objects "org.apache.activemq:*,Type=Queue" mbeans :hostname host))))))