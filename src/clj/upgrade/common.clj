(ns upgrade.common
  (:require [bidi.bidi :refer [match-route path-for]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [upgrade.encrypt EncryptionManager]))

(defn encrypt
  [key-file-path message]
  (.. (EncryptionManager.)
      (encrypt (java.io.File. key-file-path) message)))

(defn decrypt [key-file-path message]
  (.. (EncryptionManager.)
      (decrypt (java.io.File. key-file-path) message)))

;; Date Time Stuff
(defn now []
  (java.util.Date.))

(defn now-str [datetime]
  (let [sdf (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")]
    (.format sdf datetime)))

(def routes
  ["/" {"color/query" :color-query
        "color/cycle" :color-next
        #".+\.html" :resources
        #".+\.css" :resources        
        #"js/.+" :resources
        #"img/.+" :resources
        "ws" :websocket
        "hub/follows" :follower
        }])

;; TODO If/When we make this a uberjar and/or command line program,
;; path-to-file can be a cli argument. 
(defonce path-to-conf-file "config.edn")

;; TODO as of now, this just reads config for the scheduled messages
(defn read-config-from-file [path-to-file]
  (with-open [r (java.io.PushbackReader. (io/reader path-to-file))]
    (edn/read r)))

(def key-file-path "./mykeyfile")
(def log-file-path "./twitchbot.log")

(def conf
  {:log-file-path log-file-path
   :key-file-path key-file-path})

(def httpkit-conf
  {:protocol "http"
   :public-ip (decrypt
               key-file-path
               ;; Bungalow
               ;; (str "AAAADB5zClsw7Nev+SKwNyP6ijzPXHFDwFMP9lOtKO"
               ;;      "HrFT3TUJJgj4OsFWkTWA==")
               ;; Home
               (str "AAAADIu6PUorXmOZDmcRWZx5xS9SHLR9yH5ibBx1fk"
                    "mkr80J1GrP7qnVSXZ4")
               )
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
                    (str "AAAADCl+jp/Nxaj6uJ/WEjMIQ/0WTEgB72Xj"
                         "OsFOrPrIaO6U9rcVYXvQrdcSoH+ZCDeE2ngo"
                         "tqysBKOkVrtAyfvN8kw="))})

(def twitchapi-conf
  {;; clientid for the "Upgrading Dave Panel" (https://dev.twitch.tv/console)
   :clientid (decrypt key-file-path
                      (str "AAAADJlq5n9wwoVZV2I55pjIpP/I3e6J4M"
                           "f6xV/4OGvHoZCveQ3"
                           "JT9bJ9T+UR076abRqsKHnIbUDmFnUcHU="))
   :client-secret (decrypt key-file-path
                           (str "AAAADBKPEvWXOz01z71b0BtBdEcxA"
                                "OnVHjVHul/aWWjl+o"
                                "Tir6LdfALoRxsm+1xX8NFV37Ue1pjmzJIKP/w="))
   :followers-callback-url twitch-followers-callback-url
   :follow-user-id "267319958"
   :subscribe-time-in-seconds 86400
   :app-token-results
   {:access_token (decrypt key-file-path
                           (str "AAAADCqojgYlO9I/Je4Z+xDdx+eQD"
                                "ZF9NiHUhRB5RhGo7U"
                                "5wWW22JVSPL/AEQgVf39QgqaBcnqgcKQcmNB8="))
                       :expires_in 5267200,
                       :token_type "bearer"}
   })

(defn get-config []
  (-> conf
      (assoc :httpkit httpkit-conf
             :twitchapi twitchapi-conf
             :twitchbot twitchbot-conf)))

(defn log*
  [path-to-file & msgs]
  (let [prefix (str (now-str (now)) "  ")
        suffix "\n"
        msg (apply str msgs)]
    (spit path-to-file (str prefix msg suffix) :append true)))

(defn log [& msgs] (apply log* (concat [log-file-path] msgs)))

(defn transitWrite [msg]
  (with-open [out (ByteArrayOutputStream. 4096)]
    (let [writer (transit/writer out :json)]
      (transit/write writer msg)
      (.toString out))))
