(ns upgrade.common
  (:require [bidi.bidi :refer [match-route path-for]])
  (:import [upgrade.encrypt EncryptionManager]))

(defn encrypt
  [key-file-path message]
  (.. (EncryptionManager.)
      (encrypt (java.io.File. key-file-path) message)))

(defn decrypt [key-file-path message]
  (.. (EncryptionManager.)
      (decrypt (java.io.File. key-file-path) message)))

(def routes
  ["/" {"color/query" :color-query
        "color/cycle" :color-next
        #".+\.html" :resources
        #"js/.+" :resources
        "ws" :websocket
        "hub/follows" :follower
        }])

;; TODO: eventually move all of this to conf file?

(def key-file-path "./mykeyfile")
(def log-file-path "./twitchbot.log")

(def conf
  {:log-file-path log-file-path
   :key-file-path key-file-path})

(def httpkit-conf
  {:protocol "http"
   :public-ip (decrypt
               key-file-path
               "AAAADIu6PUorXmOZDmcRWZx5xS9SHLR9yH5ibBx1fkmkr80J1GrP7qnVSXZ4")
   :port 8081})

(def http-base-url 
  (let [{:keys [protocol public-ip port]} httpkit-conf]
    (str protocol "://" public-ip ":" port )))

(def twitch-followers-callback-url 
  (str http-base-url (path-for routes :follower)))

(def twitchbot-conf
   {:host "irc.chat.twitch.tv"
    :port 443
    :username "upgradingchatbot"
    :channel "#upgradingdave"
    ;; this is my user token (I think) For an app token, you have to
    ;; do the oauth handshake with client id and secret (below)
    :oauth (decrypt key-file-path
                    (str "AAAADCl+jp/Nxaj6uJ/WEjMIQ/0WTEgB72XjOsFOrPrIaO6U9rcVY"
                         "XvQrdcSoH+ZCDeE2ngotqysBKOkVrtAyfvN8kw="))})

(def twitchapi-conf
  {;; clientid for the "Upgrading Dave Panel" (https://dev.twitch.tv/console)
   :clientid (decrypt key-file-path
                      (str "AAAADJlq5n9wwoVZV2I55pjIpP/I3e6J4Mf6xV/4OGvHoZCveQ3"
                           "JT9bJ9T+UR076abRqsKHnIbUDmFnUcHU="))
   :client-secret (decrypt key-file-path
                           (str "AAAADBKPEvWXOz01z71b0BtBdEcxAOnVHjVHul/aWWjl+o"
                                "Tir6LdfALoRxsm+1xX8NFV37Ue1pjmzJIKP/w="))
   :followers-callback-url twitch-followers-callback-url
   :follow-user-id "267319958"
   :subscribe-time-in-seconds 864000
   :app-token-results
   {:access_token (decrypt key-file-path
                           (str "AAAADCqojgYlO9I/Je4Z+xDdx+eQDZF9NiHUhRB5RhGo7U"
                                "5wWW22JVSPL/AEQgVf39QgqaBcnqgcKQcmNB8=")),
                       :expires_in 5267200,
                       :token_type "bearer"}
   })

(defn get-config []
  (-> conf
      (assoc :httpkit httpkit-conf
             :twitchapi twitchapi-conf
             :twitchbot twitchbot-conf)))

(defn log
  ([msg] (log log-file-path msg))
  ([path-to-file msg]
   (spit path-to-file (str msg "\n") :append true)))

