(ns monitor.udpclient
  (:import (java.net DatagramSocket DatagramPacket InetAddress)))

(defmacro with-socket
  "Execute a body in the context of a socket."
  [socket & body]
  `(let [socket# (DatagramSocket.)
         ~socket socket#]
     (try
       ~@body
       (finally
        (.close socket#)))))

(defn send-packet
  "Send a data packet to and ip and port."
  [socket data hostname port]
  (let [addr (InetAddress/getByName hostname)
        package (DatagramPacket. data (count data) addr port)]
    (.send socket package)))