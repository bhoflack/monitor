(ns monitor.core
  (:import java.io.File
           javax.management.remote.JMXConnector)
  (:require [clojure.java.jmx :as jmx]
            [clojure.edn :as edn])
  (:gen-class :name monitor.bin))

(def brokers (atom nil))
(def results (agent nil))

(def thread (atom nil))
(def stop-thread (atom false))
(def attributes (atom [:AverageEnqueueTime
                       :QueueSize
                       :ProducerCount
                       :MemoryUsagePortion
                       :MemoryPercentUsage
                       :MaxEnqueueTime
                       :InFlightCount
                       :EnqueueCount
                       :DispatchCount
                       :DequeueCount
                       :CursorMemoryUsage
                       :CursorFull
                       :ConsumerCount]))

(def sleep-time (atom 60000))

(defn read-attributes
  [attributes type opts]
  (jmx/with-connection opts
    (->> (jmx/mbean-names type)
         (map #(.toString %))
         (map (fn [name] [name (jmx/read name attributes)]))
         (doall))))

(defn assoc-append
  [hm k v]
  (assoc hm k (cons v (get hm k))))

(defn append-attributes
  [results attributes type broker]
  (assoc-append results
                (:host broker)
                (read-attributes attributes type broker)))

(defn write!
  [filename]
  (->> @results
       (prn-str)
       (spit filename)))

(defn read!
  [filename]
  (->> (slurp filename)
       (edn/read-string)
       (send results merge)))

(defn start!
  [type filename]
  (reset! stop-thread false)
  (reset! thread
          (Thread.
           (fn []
             (doall
              (while (not @stop-thread)
                (write! filename)                
                (doseq [broker @brokers]
                  (println "Polling broker " broker)
                  (send results append-attributes @attributes type broker))
                (Thread/sleep @sleep-time))))))
  (.start @thread))

(defn stop!
  []
  (reset! stop-thread true))

(defn file-exists?
  [filename]
  (-> filename
      (File.)
      (.exists)))

(defn- init-state!
  [filename]
  (if (file-exists? filename)
    (read! filename)))

(defn- set-broker!
  [brokers-config]
  (->> brokers-config
       (map (fn [broker]
              {:host (:host broker)
               :port (:port broker)
               :jndi-path (:jndi-path broker)
               :environment {JMXConnector/CREDENTIALS (into-array String (:credentials broker))}}))
       (doall)
       (reset! brokers)))

(defn- init-config!
  [filename]
  (let [config (->> (slurp filename)
                    (edn/read-string))]
    (set-broker! (:brokers config))
    (if (:attributes config)
      (reset! attributes (:attributes config)))
    (if (:sleep-time config)
      (reset! sleep-time (:sleep-time config)))))
    
(defn -main
  ([]
     (-main "monitor.edn"))
  ([filename]
     (-main "properties.edn" filename))  
  ([properties filename]
     (init-state! filename)
     (if (file-exists? properties)
       (init-config! properties)
       (throw (Exception. "Properties file is required")))
     (start! "*:Type=Queue,*" filename)))
