(ns upgrade.web.http
  (:require [clojure.java.io :as io]
            [upgrade.common :refer [log]]
            [org.httpkit.server :refer [run-server]]
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

(defn file-handler [path-to-file]
  (fn [request]
    (res/response (io/file (str "resources/public/" path-to-file)))))

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
        "index.html" (file-handler "index.html")
        #"js/.+" resources-handler
        }])

(def handler
  (wrap-json-response
   (make-handler routes)))

(defn app [req]
  (handler req))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (run-server #'app {:port 8081})))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (start-server))

;;(run-server app {:port 8080})

