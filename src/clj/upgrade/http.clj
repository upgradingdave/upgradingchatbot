(ns upgrade.http
  (:require [clojure.java.io :as io]
            [upgrade.common :refer [log]]
            [org.httpkit.server :as httpkit]
            [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.mock.request :as mock]))

(defonce http-state (atom {:color "purple"}))

(def available-colors {0 "purple"
                       1 "red"
                       2 "green"
                       3 "blue"
                       4 "orange"
                       5 "black"
                       6 "white"})

(defn find-color-idx [color]
  (first (first (filter (fn [[k v]] (= v color)) available-colors))))

(defn next-color [color available-colors]
  (let [idx (find-color-idx color)]
    (if (>= idx (dec (count available-colors)))
      (get available-colors 0)
      (get available-colors (inc idx)))))

(defn color-query-handler
  [request]
  "Return the current color. wrap-json-response will automatically
  convert clojure in body into json"
  (res/response @http-state))

(defn next-color-handler
  [request]
  "Return the next color."
  (swap! http-state assoc :color (next-color (:color @http-state) available-colors))
  (res/response @http-state))

(defn resources-handler
  [request]
  (let [uri (:uri request)
        path-to-file (str "resources/public" uri)
        file (io/file path-to-file)]
    (if (and file (.exists file))
      (res/response file)
      (res/response "file not found"))
    ))

(def routes
  ["/" {"color/query" color-query-handler
        "color/cycle" next-color-handler
        #".+\.html" resources-handler
        #"js/.+" resources-handler
        }])

(def handler
  (wrap-json-response
   (make-handler routes)))

(defn app [req]
  (handler req))

;; The #' is useful when you want to hot-reload code
;; You may want to take a look: https://github.com/clojure/tools.namespace
;; and http://http-kit.org/migration.html#reload
(defn run-server [port]
  (httpkit/run-server #'app {:port port}))

(defn start-httpkit! [{port :port :as httpkit}]
  (assoc httpkit
         :server (run-server port)
         :running? true))

(defn stop-httpkit! [{server :server :as httpkit}]
  (when-not (nil? server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (assoc httpkit
           :server (server :timeout 100)
           :running? false)))
