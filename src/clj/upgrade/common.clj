(ns upgrade.common
  (:import [upgrade.encrypt EncryptionManager]))

(defn encrypt
  [key-file-path message]
  (.. (EncryptionManager.)
      (encrypt (java.io.File. key-file-path) message)))

(defn decrypt [key-file-path message]
  (.. (EncryptionManager.)
      (decrypt (java.io.File. key-file-path) message)))

(defn get-config []
  (let [key-file-path "./mykeyfile"]
    {:log-file-path "./twitchbot.log"
     :key-file-path key-file-path
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

