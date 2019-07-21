(ns upgrade.http
  (:require [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [buddy.sign.jwt :as jwt]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [hiccup.core :refer [html]]
            [org.httpkit.server :as httpkit :refer [send! with-channel]]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.mock.request :as mock]
            [upgrade.common :refer [log routes transitWrite get-config]]
            [upgrade.freesound :refer [search-and-play-file!]]
            [upgrade.twitchbot :refer [send-message ws-clients]]
            [upgrade.twitch :refer [active-follower-subscription?
                                    get-webhook-subscriptions
                                    subscribe-to-follows
                                    unsubscribe-to-follows
]]
            ))

(defonce http-state (atom {:color "purple"
                           :twitchbot nil}))

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
  (res/response (:color @http-state)))

(defn next-color-handler
  "Return the next color."
  [request]
  (log "HTTPKIT: Attempting to find next color")
  (log request)
  (let [headers (:headers request)
        auth-header (get headers "authorization")]
    (log auth-header))
  (swap! http-state assoc :color (next-color @http-state available-colors))
  (res/response (:color @http-state)))

(defn resources-handler
  [request]
  (let [uri (:uri request)
        path-to-file (str "resources/public" uri)
        file (io/file path-to-file)]
    (if (and file (.exists file))
      (res/response file)
      (res/response "file not found"))
    ))

(defn play-follower-animation [client channel from_name]

  ;; Really annoying festival music! (play-sound! 468218)
  (search-and-play-file! "drunkensailor")
  
  (doseq [ch @ws-clients]
    (send! ch (transitWrite {:animation-key :followers
                             :follower from_name})))
  (when client
    (send-message
     client channel
     (str "Welcome, " from_name "! Thanks for following!!!"))))

;; TODO maybe get the unique notification id from twitch (maybe it's
;; in the header?)
(defn follower-handler
  "This is used for 2 purposes: 1. Twitch will send a challenge when we
  first subscribe to follower webhook, so we need to respond with
  challenge. 2. Twitch will send a GET request to this endpoint
  anytime someone follows me, we need to respond with a 200 status
  response."
  [{:keys [query-params body] :as request}]
  (log "Processing 'hub/follows' request ...")

  (let [hub-challenge (get query-params "hub.challenge")
        new-followers (:data body)]

    (cond

      ;; this is the first subscription challenge, respond to hub
      ;; challenge
      hub-challenge
      (do
        (log (str "Responding to twitch with challenge: " hub-challenge))
        (res/response hub-challenge))

      ;; we got a new follower    
      new-followers
      (do
        ;; (log new-followers)
        (doseq [follower new-followers]
          ;; (log follower)
          (let [{:keys [followed_at from_id from_name]} follower
                {:keys [client channel]} (get-in @http-state
                                                 [:twitchbot])]
            (when from_name
              (play-follower-animation client channel from_name)
              (log (str "Got a new Follower! Username: " from_name)))))
        
        ;; send 200 response to twitch
        (res/response "Thanks, Twitch!"))


      ;; If this is just someong browsing this endpoint, display
      ;; statistics about follower subscriptions
      :else
      (let [conf (get-config)
            clientid (get-in conf [:twitchapi :clientid])
            app-token (get-in conf [:twitchapi :app-token-results
                                    :access_token])
            result (get-webhook-subscriptions clientid app-token)
            total-subs (get result :total 0)
            ;; subs (map (fn [sub] ) (:data result))
            
            ]
        
        (res/response
         (html
          [:div
           [:div "Twitch webhook endpoint is up!"]
           [:div "Total Active Follower Subscriptions: " total-subs]
           [:ul
            (map (fn [sub] [:li [:p (:callback sub)]
                            [:p (:expires_at sub)]]) (:data result))]]
          ))))))

(defn websocket-handler [request]
  (with-channel request channel
    (swap! ws-clients conj channel)))

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

(defn bidi-routes-to-handlers
  "Maps a bidi handler keyword (defined in routes) to a handler function"
  [kw]
  (case kw
    :color-query color-query-handler
    :color-next next-color-handler
    :resources resources-handler
    :websocket websocket-handler
    :follower follower-handler))

(def handler
  (-> (make-handler routes bidi-routes-to-handlers)
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-params)
      ;;TODO can remove wrap-cors unless testing in twitch developer rig
      (wrap-cors)
      ))

(defn app [req]
  (handler req))

;; The #' is useful when you want to hot-reload code You may want to
;; take a look: https://github.com/clojure/tools.namespace and
;; http://http-kit.org/migration.html#reload
(defn run-server [port]
  (log port)
  (httpkit/run-server #'app {:port port}))

;; TODO: add webhook subscription id to system state
(defn start-httpkit!
  "Start a httpkit server, updates twitchbot dependency and subscribes
  to twitch webhook notifications"
  [{:keys [twitchbot httpkit twitchapi] :as system}]
  (let [{:keys [port]} httpkit
        {:keys [clientid
                follow-user-id
                followers-callback-url
                subscribe-time-in-seconds
                app-token-results]} twitchapi
        app-token (:access_token app-token-results)]
    (swap! http-state assoc :twitchbot twitchbot)
    (log "Attempting to start http server on port: " port " ...")
    (let [server (run-server port)
          ;; check if we have an active follower subscription
          result (get-webhook-subscriptions clientid app-token)
          subscribed? (active-follower-subscription?
                       followers-callback-url
                       follow-user-id
                       result)]

      (if-not subscribed?
        (subscribe-to-follows clientid
                              follow-user-id
                              followers-callback-url
                              subscribe-time-in-seconds))

      (assoc httpkit :server server))))

;; TODO: remove webhook subscription id to system state
(defn stop-httpkit! [{:keys [httpkit twitchapi] :as system}]
  "Stop httpkit server, invalidate twitchbot dependency and
  unsubscribe from twitch webhooks."
  (let [{:keys [server]} httpkit
        {:keys [clientid
                follow-user-id
                followers-callback-url
                subscribe-time-in-seconds]} twitchapi]

    (swap! http-state assoc :twitchbot nil)

    (unsubscribe-to-follows clientid follow-user-id
                            followers-callback-url
                            subscribe-time-in-seconds)

    (if (nil? server)
      httpkit
      ;; else
      ;; graceful shutdown: wait 100ms for existing requests to be finished
      ;; :timeout is optional, when no timeout, stop immediately
      (assoc httpkit :server (server :timeout 100)))))


(comment
  (def conf (get-config))
  (def clientid (get-in conf [:twitchapi :clientid]))
  (def client-secret (get-in conf [:twitchapi :client-secret]))
  (def app-token (get-in conf [:twitchapi :app-token-results :access_token]))
  (def follow-user-id (get-in conf [:twitchapi :follow-user-id]))
  (def followers-callback-url (get-in conf [:twitchapi :followers-callback-url]))

    (def subscribe-time-in-seconds (get-in conf [:twitchapi :subscribe-time-in-seconds]))


  (def client (get-in conf [:twitchbot :client]))
  (def channel (get-in conf [:twitchbot :channel]))

  (doseq [ch @ws-clients]
    (send! ch (transitWrite {:animation-key :chat
                             :msg "test chat"})))

  
  )
