(ns monitor.graphite
  (:require [monitor.udpclient :as udp]))

(defn send-data
  "Send data to the graphite server"
  [path data timestamp & {:keys [hostname port]
                          :or {hostname "localhost"
                               port 2003}}]
  (udp/with-socket socket
    (let [ts (.getTime timestamp)]
      (udp/send-packet socket (.getBytes (str path " " data " " ts "\n")) hostname port))))