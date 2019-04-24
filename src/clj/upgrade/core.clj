(ns upgrade.core
  (:import [org.pircbotx Configuration Configuration$Builder PircBotX]
           [upgrade.encrypt EncryptionManager]))

(defn decrypt [message]
  (.. (EncryptionManager.) (decrypt (java.io.File. "./mykeyfile") message)))

(defn get-config []
  {:username "upgradingdave"
   :oauth (decrypt "AAAADPPnM4SgS6br8YQs2T4zGRzNChlE+cN2uoYhmu465YwIS0V80NO0Si4Cst2sYHoijSpVT2ymyisZptKuuBAf+Xs=")
   :channel "upgradingdave"
   :server "irc.chat.twitch.tv"
   :port 6667
   })

(defn create-listener []
  (proxy [org.pircbotx.hooks.ListenerAdapter] []
    (onPing [event] (println (. event (getPingValue))))))

(defn create-irc-client [conf]
  (let [config (.. (Configuration$Builder.)
                   (setName (:username conf))
                   (setServer (:server conf) (:port conf))
                   (setServerPassword (:oauth conf))
                   (addAutoJoinChannel (str "#" (:channel conf)))
                   (addListener (create-listener))
                   (buildConfiguration))]
    (PircBotX. config)))



(defn -main []
  (println "Attempting to start twitch chat bot ... ")
  (let [client (create-irc-client (get-config))]
    (.startBot client)))



