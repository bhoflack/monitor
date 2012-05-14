(ns monitor.web
  (:use monitor.event
        compojure.core
        ring.adapter.jetty
        hiccup.core
        hiccup.page)
  (:require [compojure.route :as route]))

(defn view-layout [& content]
  (html
   (doctype :xhtml-strict)
   (xhtml-tag "en"
              [:head
               [:meta {:http-equiv "Content-type"
                       :content "text/html; charset=utf-8"}]
               [:title "JMX Monitor"]]
              [:body content])))

(defn hosts-page
  "Page with all hosts"
  ([]
     (let [hosts (find-hosts)]
       (view-layout
        [:ul
         (for [host hosts]
           [:li [:a {:href (str "/host/" host)} host]])]))))

(defn timestamps-page
  "Page with all measured timestamps for a host"
  [hostname]
  (let [keys (find-keys-for-host hostname)
        keys1 (map :key keys)
        events (find-events-for-host hostname)]
    (view-layout
     [:h2 "available keys"]
     [:ul
      (for [key keys1]
        [:li [:a {:href (str "/host/" hostname "/key/" key)} key]])]
     [:h2 "events measured"]
     [:ul
      (for [event events]
        [:li [:a {:href (str "/host/" hostname "/event/" event)} event]])])))

(defn mbeans-page
  "Page with all means for a host and timestamp"
  [hostname timestamp]
  (let [mbeans (find-mbeans-for-host-timestamp hostname timestamp)]
    (view-layout
     [:ul
      (for [mbean mbeans]
        [:li [:a {:href (str "/host/" hostname "/event/" timestamp "/" (:id mbean))} (:uri mbean)]])])))

(defn data-page
  "Page with all data for a mbean, host and timestamp"
  [hostname timestamp uri]
  (let [id (Integer/parseInt uri)
        data (find-data-for-host-timestamp-id hostname timestamp id)]
    (println data)
    (view-layout
     [:table
      [:tr
       [:th "Key"]
       [:th "Value"]]
      (doall (for [d data]
               [:tr
                [:td (:key d)]
                [:td (:value d)]]))])))

(defn attribute-page
  "Page with all values for all uris for a given attribute"
  [hostname key]
  (let [data (find-latest-attribute-data-for-host hostname key)]
    (view-layout
     [:table
      [:tr
       [:th "URI"]
       [:th "Key"]
       [:th "Value"]]
      (doall (for [d data]
               [:tr
                [:td (:uri d)]
                [:td [:a {:href (str "/host/" hostname "/key/" (:key d) "/trend/" (:uriid d))} (:key d)]]
                [:td (:value d)]]))])))

(defn attribute-trend-page
  "Show the trend for a given host, uri and attribute"
  [hostname uriid key]
  (let [uriid1 (Integer/parseInt uriid)
        data (find-trend-for-host-uriid-attribute hostname uriid1 key)]
    (view-layout
     [:table
      [:tr
       [:th "Timestamp"]
       [:th "Key"]
       [:th "Value"]]
      (doall (for [d data]
               [:tr
                [:td (:timestamp d)]
                [:td (:key d)]
                [:td (:value d)]]))])))

(defroutes app
  (GET "/" [] (hosts-page))
  (GET "/host/:hostname" [hostname] (timestamps-page hostname))
  (GET "/host/:hostname/key/:key" [hostname key] (attribute-page hostname key))
  (GET "/host/:hostname/key/:key/trend/:uri" [hostname key uri] (attribute-trend-page hostname uri key))
  (GET "/host/:hostname/event/:timestamp" [hostname timestamp] (mbeans-page hostname timestamp))
  (GET "/host/:hostname/event/:timestamp/:uri" [hostname timestamp uri] (data-page hostname timestamp uri))
  (route/not-found "<h1>Page not found</h1>"))




