(ns upgrade.core
  (:import [net.engio.mbassy.listener Handler]
           [org.kitteh.irc.client.library Client]
           [org.kitteh.irc.client.library.event.channel
            ChannelMessageEvent
            ChannelNoticeEvent
            ChannelJoinEvent]
           [org.kitteh.irc.client.library.event.client
            ClientReceiveCommandEvent
            ClientReceiveNumericEvent]
           [org.kitteh.irc.client.library.feature.twitch TwitchSupport]
           [upgrade.encrypt EncryptionManager]
           ))

(defn encrypt [message]
  (.. (EncryptionManager.) (encrypt (java.io.File. "./mykeyfile") message)))

(defn decrypt [message]
  (.. (EncryptionManager.) (decrypt (java.io.File. "./mykeyfile") message)))

(defn get-config []
  {:log-file-path "./twitchbot.log"
   :server "irc.chat.twitch.tv"
   :port 443
   :username "upgradingchatbot"
   :channel "#upgradingdave"
   :oauth (decrypt "AAAADCl+jp/Nxaj6uJ/WEjMIQ/0WTEgB72XjOsFOrPrIaO6U9rcVYXvQrdcSoH+ZCDeE2ngotqysBKOkVrtAyfvN8kw=")
   })

(defn log [path-to-file msg]
  (let [conf (get-config)]
    (spit path-to-file (str msg "\n") :append true)))

(defn handle-event [evt]
  "Here's the main event handler. This is where we can implement cool features"
  (let [conf (get-config)
        log-file-path (:log-file-path conf)
        log (partial log log-file-path)
        event-type (type evt)
        ;; Seems like class is same as type
        ;; event-class (class evt)
        ]
    (cond

      (instance? ChannelMessageEvent evt)
      (let [msg (. evt getMessage)
            server-msg (. (. evt getSource) (getMessage))]
        (. evt (sendReply (str "Yeah, yeah, I heard you. You said: " msg))))
      
      :else
      (log (str "NEED IMPLEMENTATION FOR: " event-type)))))

;; Make a class with methods for handling events
(defprotocol IrcListeners

  "Methods for responding to irc events"
  (^{Handler true} listen-for-all-events [this evt] "Listen for any event"))

(deftype IrcEventHanders []
  IrcListeners
  (^{Handler true}   
   listen-for-all-events [this evt] (handle-event evt)))

(defn create-client [conf]
  (let [client (.. (Client/builder)
                   (server)
                   (host (:server conf))
                   (port (:port conf))
                   (password (:oauth conf))
                   (then)
                   (nick (:username conf))
                   (build))]
    (TwitchSupport/addSupport client)))

(defn add-listeners [client]
  (let [evt-mgr (. client (getEventManager))]
    (. evt-mgr (registerEventListener (IrcEventHanders.)))))

(defn send-message
  ([client message]
   (let [conf (get-config)
         channel (:channel conf)]
     (send-message client channel message)))
  ([client channel message]
   (. client (sendMessage channel message))))

(defn leave-and-disconnect [client channel]
  (. client (removeChannel channel "Later alligators"))
  (. client (shutdown "Upgradingdavebot is shutting down")))

(comment

  (def conf (get-config))
  (def channel (:channel conf))
  (def client (create-client conf))
  
  (add-listeners client)
  (. client (connect))
  (. client addChannel (into-array String [channel]))
  (send-message client "UpgradingBot is ALIVE")

  (leave-and-disconnect client channel)

  )

(defn -main []
  (println "Attempting to start twitch chat bot ... ")
)



