(ns monitor.event
  (:require [clojure.java.jdbc :as sql]))

(def ds {:subprotocol "postgresql"
         :subname "//localhost/monitor"
         :user "monitor"
         :password "monitor"
         :classname "org.postgresql.Driver"})

(def sequence-ddl "CREATE SEQUENCE seqid")

(def host-ddl (sql/create-table-ddl
               :host
               [:id "integer" "PRIMARY KEY"]
               [:name "varchar(255)"]))

(def event-ddl (sql/create-table-ddl
                :event
                [:id "integer" "PRIMARY KEY"]
                [:hostid "integer"]
                [:timestamp "varchar(255)"]))

(def event-for-uri-ddl (sql/create-table-ddl
                        :eventforuri
                        [:id "integer" "PRIMARY KEY"]
                        [:eventid "integer"]
                        [:uri "varchar(255)"]))

(def event-data-ddl (sql/create-table-ddl
                     :eventdata
                     [:id "integer" "PRIMARY KEY"]
                     [:eventforuriid "integer"]
                     [:key "varchar(255)"]
                     [:value "varchar(255)"]))

(defn table-exists?
  [table]
  (sql/with-connection ds
    (sql/with-query-results rs ["select * from information_schema.tables where upper(table_name) = ?" (.toUpperCase (name table))]
      (not (empty? rs)))))

(defn sequence-exists?
  [index]
  (sql/with-connection ds
    (sql/with-query-results rs ["select * from information_schema.sequences where upper(sequence_name) = ?" (.toUpperCase (name index))]
      (not (empty? rs)))))

(defn create-conditional
  [table ddl]
  (sql/with-connection ds
    (if (not (table-exists? table))
      (do (println "creating table" table)
          (sql/do-commands ddl)))))

(defn init-db []
  (if (not (sequence-exists? :seqid))
    (sql/with-connection ds
      (sql/do-commands sequence-ddl)))

  (doall (map (fn [[table ddl]] (create-conditional table ddl)) [[:host host-ddl]
                                                                 [:event event-ddl]
                                                                 [:eventforuri event-for-uri-ddl]
                                                                 [:eventdata event-data-ddl]])))

(defn first-result
  [sql-params]
  (sql/with-query-results rs sql-params
    (first rs)))

(defn next-id []
  (sql/with-connection ds
    (sql/with-query-results rs ["select nextval('seqid') as id"]
      (:id (first rs)))))


(defn create-host-conditional!
  "Create a host if it doesn't exist"
  [hostname]
  (sql/with-connection ds
    (sql/with-query-results rs ["select id, name from host where name=?" hostname]
      (if (empty? rs)
        (do
          (sql/insert-record :host {:id (next-id)
                                    :name hostname})
          (first-result ["select id, name from host where name=?" hostname]))
        (first rs)))))

(defn create-event!
  [hostid timestamp]
  (sql/with-connection ds
    (sql/with-query-results rs ["select id, hostid, timestamp from event where hostid=? and timestamp=?" hostid timestamp]
      (if (empty? rs)
        (do
          (sql/insert-record :event {:id (next-id)
                                     :hostid hostid
                                     :timestamp timestamp})
          (first-result ["select id, hostid, timestamp from event where hostid=? and timestamp=?" hostid timestamp]))
        (first rs)))))

(defn create-event-uri!
  [eventid uri]
  (sql/with-connection ds
    (sql/insert-record :eventforuri {:id (next-id)
                                     :eventid eventid
                                     :uri uri})
    (first-result ["select id, eventid, uri from eventforuri where eventid = ? and uri = ?" eventid uri])))

(defn create-event-data-uri!
  [eventforuriid key value]
  (sql/with-connection ds
    (sql/insert-record :eventdata {:id (next-id)
                                   :eventforuriid eventforuriid
                                   :key key
                                   :value value})))

(defn save!
  "Save an event to the event db"
  [hostname timestamp uri values]
  (let [host-id (:id (create-host-conditional! hostname))
        event-id (:id (create-event! host-id timestamp))
        event-for-uri-id (:id (create-event-uri! event-id uri))
        keys [:MaxEnqueueTime :InFlightCount :DequeueCount :QueueSize :EnqueueCount :AverageEnqueueTime :ConsumerCount]]
    (doall (map (fn [k]
                  (let [key (name k)
                        v (get values k)
                        value (str v)]
                    (create-event-data-uri! event-for-uri-id key value)))
                keys))))

(defn find-hosts
  "Find the hosts in the db"
  []
  (sql/with-connection ds
    (sql/with-query-results res ["select name from host"]
      (doall (map :name res)))))

(defn find-events-for-host
  "Find the events timestamps logged for a host"
  [name]
  (sql/with-connection ds
    (sql/with-query-results res ["select timestamp from host h inner join event e on h.id = e.hostid where h.name = ?" name]
      (doall (map :timestamp res)))))

(defn find-mbeans-for-host-timestamp
  "Find the mbeans monitored for a timestamp"
  [host timestamp]
  (sql/with-connection ds
    (sql/with-query-results res
      ["select u.id as id, uri
        from host h
          inner join event e
          on h.id = e.hostid
            inner join eventforuri u
            on e.id = u.eventid
         where name = ?
           and timestamp = ?" host timestamp]
      (doall res))))

(defn find-data-for-host-timestamp-uri
  "Find the data monitored for a host timestamp and uri"
  [host timestamp uri]
  (sql/with-connection ds
    (sql/with-query-results res
      ["select key value
        from host h
          inner join event e
           on h.id = e.hostid
            inner join eventforuri u
            on e.id = u.eventid
              inner join eventdata d
              on u.id = d.eventforuriid
         where name = ?
           and timestamp = ?
           and uri = ?" host timestamp uri]
      (doall res))))

(defn find-data-for-host-timestamp-id
  "Find the data monitored for a host timestamp and id"
  [host timestamp id]
  (sql/with-connection ds
    (sql/with-query-results res
      ["select key, value
        from host h
          inner join event e
           on h.id = e.hostid
            inner join eventforuri u
            on e.id = u.eventid
              inner join eventdata d
              on u.id = d.eventforuriid
         where name = ?
           and timestamp = ?
           and u.id = ?" host timestamp id]
      (doall res))))