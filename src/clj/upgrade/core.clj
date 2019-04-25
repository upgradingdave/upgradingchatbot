(ns upgrade.core
  (:import [org.kitteh.irc.client.library Client]
           [org.kitteh.irc.client.library.event.channel ChannelJoinEvent]
           [org.kitteh.irc.client.library.feature.twitch TwitchSupport]
           [upgrade.encrypt EncryptionManager]
           ))

(defn decrypt [message]
  (.. (EncryptionManager.) (decrypt (java.io.File. "./mykeyfile") message)))

(defn get-config []
  {:username "upgradingdave"
   :oauth (decrypt "AAAADPPnM4SgS6br8YQs2T4zGRzNChlE+cN2uoYhmu465YwIS0V80NO0Si4Cst2sYHoijSpVT2ymyisZptKuuBAf+Xs=")
   :channel "upgradingdave"
   :server "irc.chat.twitch.tv"
   :port 443
   })

(defn create-client [conf]
  (let [client (.. (Client/builder)
                   (server)
                   (host (:server conf))
                   (port (:port conf))
                   (password (:oauth conf))
                   (then)
                   (nick (:username conf))
                   (build))]

    (-> client (TwitchSupport/addSupport)
        (. (connect)))
    client))

(defn -main []
  (println "Attempting to start twitch chat bot ... ")
  (let [conf (get-config)
        channel (str "#" (:channel conf))
        client (create-client conf)]
    (. client (addChannel (into-array String [channel])))
    (. client (sendMessage channel "Upgradingdave's Bot is ALIVE!"))))



