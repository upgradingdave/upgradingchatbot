(ns upgrade.http
  (:require [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [buddy.sign.jwt :as jwt]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [org.httpkit.server :as httpkit :refer [send! with-channel]]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.mock.request :as mock]
            [upgrade.common :refer [log]]
            [upgrade.twitchbot :refer [send-message]]
            ))

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

;; TODO maybe get the unique notification id from twitch (maybe it's in the header?)
(defn follower-handler
  "Twitch will send a GET request to this endpoint anytime someone follows me"
  [{:keys [query-params body] :as request}]
  (if-let [hub-challenge (get query-params "hub.challenge")]
    ;; respond to hub challenge
    (do
      (log (str "Responding to twitch with challenge: " hub-challenge))
      (res/response hub-challenge))
    ;; otherwise, we got a new follower!!
    (let [new-followers (:data body)]
      (doseq [follower new-followers]
        (let [{:keys [followed_at from_id from_name]} follower]
          (log (str "Got a new Follower: " from_name "!"))))))
  (res/response "This is a twitch webhook endpoint"))

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
        "hub/follows" follower-handler
        }])

;; Access-Control-Allow-Origin
(defn wrap-extra-headers
  "Ring handler that adds extra headers to every request"
  [handler extra-headers]
  (fn [request]
    (let [response (handler request)]
         (update-in response [:headers]
                    (fn [headers] (merge headers extra-headers))))))

;; TODO remove all this for production,
;; but we need it for the developer rig
(defn wrap-cors
  "Ring handler that adds headers to make CORs happy for every request"
  [handler]
  (wrap-extra-headers handler
    {"Access-Control-Allow-Origin" "*"
     "Access-Control-Allow-Credentials", "true"
     "Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT"
     "Access-Control-Allow-Headers", (str "Access-Control-Allow-Headers, "
                                          "Origin,Accept, X-Requested-With, "
                                          "Content-Type, "
                                          "Access-Control-Request-Method, "
                                          "Access-Control-Request-Headers, "
                                          "Authorization")}))

(def handler
  (-> (make-handler routes)
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-params)
      ;;TODO can remove wrap-cors unless testing in twitch developer rig
      (wrap-cors)
      ))

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
