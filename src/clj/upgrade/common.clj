(ns upgrade.common
  (:import [upgrade.encrypt EncryptionManager]))

(defn encrypt
  [key-file-path message]
  (.. (EncryptionManager.)
      (encrypt (java.io.File. key-file-path) message)))

(defn decrypt [key-file-path message]
  (.. (EncryptionManager.)
      (decrypt (java.io.File. key-file-path) message)))

;; TODO eventually move this to a config file
(defn get-config []
  (let [key-file-path "./mykeyfile"]
    {:log-file-path "./twitchbot.log"
     :key-file-path key-file-path

     :httpkit
     {:public-ip (decrypt
                  key-file-path
                  "AAAADIu6PUorXmOZDmcRWZx5xS9SHLR9yH5ibBx1fkmkr80J1GrP7qnVSXZ4")
      :port 8081}

     :twitchapi
     {;; clientid for the "Upgrading Dave Panel" (https://dev.twitch.tv/console)
      :clientid (decrypt key-file-path
                         (str "AAAADJlq5n9wwoVZV2I55pjIpP/I3e6J4Mf6xV/4OGvHoZCveQ3"
                              "JT9bJ9T+UR076abRqsKHnIbUDmFnUcHU="))}
     
     :twitchbot
     {:host "irc.chat.twitch.tv"
      :port 443
      :username "upgradingchatbot"
      :channel "#upgradingdave"
      :oauth (decrypt key-file-path
                      (str "AAAADCl+jp/Nxaj6uJ/WEjMIQ/0WTEgB72XjOsFOrPrIaO6U9rcVY"
                           "XvQrdcSoH+ZCDeE2ngotqysBKOkVrtAyfvN8kw="))}}))

(defn log
  ([msg] (log (:log-file-path (get-config)) msg))
  ([path-to-file msg]
   (spit path-to-file (str msg "\n") :append true)))

