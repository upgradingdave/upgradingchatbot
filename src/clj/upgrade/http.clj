(ns upgrade.http
  (:require [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [buddy.sign.jwt :as jwt]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [org.httpkit.server :as httpkit :refer [send! with-channel]]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.mock.request :as mock]
            [upgrade.common :refer [log]]))

(defonce http-state (atom "purple"))

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
  "Return the current color. wrap-json-response will automatically
  convert clojure in body into json"
  [request]
  (log "HTTPKIT: Attempting to get current color")
  (res/response @http-state))

(defn next-color-handler
  "Return the next color."
  [request]
  (log "HTTPKIT: Attempting to find next color")
  (log request)
  (let [headers (:headers request)
        auth-header (get headers "authorization")]
    (log auth-header)
    )
  (reset! http-state (next-color @http-state available-colors))
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

(defonce ws-clients (atom #{}))

(defn websocket-handler [request]
  (with-channel request channel
    (swap! ws-clients conj channel)))

(def routes
  ["/" {"color/query" color-query-handler
        "color/cycle" next-color-handler
        #".+\.html" resources-handler
        #"js/.+" resources-handler
        "ws" websocket-handler 
        }])

;; Access-Control-Allow-Origin
(defn wrap-extra-headers [handler extra-headers]
  (fn [request]
    (let [response (handler request)]
         (update-in response [:headers]
                    (fn [headers] (merge headers extra-headers))))))

(def handler
  (wrap-json-response
   (wrap-extra-headers
    (make-handler routes)
    ;; remove all this for production,
    ;; but we need it for the developer rig
    {"Access-Control-Allow-Origin" "*"
     "Access-Control-Allow-Credentials", "true"
     "Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT"
     "Access-Control-Allow-Headers", (str "Access-Control-Allow-Headers, "
                                          "Origin,Accept, X-Requested-With, "
                                          "Content-Type, "
                                          "Access-Control-Request-Method, "
                                          "Access-Control-Request-Headers, "
                                          "Authorization")})))

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
