(ns monitor.web
  (:use monitor.core, compojure.core, ring.adapter.jetty, hiccup.core)
  (:require [compojure.route :as route]))

(defroutes main-routes
  (GET "/" []
       """<html>
            <head>
              <title>Monitor queues</title>
            </head>
            <body>
              <

