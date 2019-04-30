(ns upgrade.twitchbot.core
  (:require [upgrade.twitchbot.freesound :refer [search-and-play-nth players-stop]]
            [upgrade.twitchbot.common :refer [decrypt]])
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
           ))

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
  "Here's the main event handler. This is where we can implement fun stuff"
  (let [conf (get-config)
        channel (:channel (get-config))
        log-file-path (:log-file-path conf)
        log (partial log log-file-path)
        event-type (type evt)
        ;; Seems like class is same as type
        ;; event-class (class evt)
        ]
    (cond

      ;; Do stuff when a message was sent to the channel
      (instance? ChannelMessageEvent evt)
      (let [msg (. evt getMessage)
            server-msg (. (. evt getSource) (getMessage))
            [_ command] (re-matches #"^(!\w+).*$" msg)]
        (cond

          (= command "!help")
          (do
            (.sendReply evt "Welcome! The UpgradingChatBot is online. Type !help for a full list of commands. Feel free to play around and have fun! All commands start with an exclamation point (!). For example, try '!play <search-term>' to play a sound.")
            ;; Either not calling sendMessage with correct params or
            ;; connection is closed after sendReply?
            ;; (let [client (.getClient evt)]
            ;;   (.sendMessage client channel "!play <sound>"))
            ;; Can't send multiple replies?
            ;;(.sendReply evt "Try typing: `!play <search>` for some sound effects")
            )

          (= command "!play")
          (do 
            (if-let [[_ search-term n] (re-matches #"^!play\s+\"([^\"]+)\"\s+(\d+)$" msg)]
              (do
                (println "Number 1")
                (search-and-play-nth search-term (Integer/parseInt n))))

            (if-let [[_ search-term n] (re-matches #"^!play\s+([^\"]+)\s+(\d+)$" msg)]
              (do
                (println "Number 2")
                (search-and-play-nth search-term (Integer/parseInt n))))

            (if-let [[_ search-term n] (re-matches #"^!play\s+\"([^\s]+)\"$" msg)]
              (do
                (println "Number 3")
                (search-and-play-nth search-term 0)))

            (if-let [[_ search-term n] (re-matches #"^!play\s+([^\s]+)$" msg)]
              (do
                (println "Number 4")
                (search-and-play-nth search-term 0))))

          (= command "!stop")
          (players-stop)
 
          :else
          (log (str "ChatBot Heard: " msg))
          
          ))
      
      ;; :else
      ;; (log (str "NEED IMPLEMENTATION FOR: " event-type))
      )))

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



