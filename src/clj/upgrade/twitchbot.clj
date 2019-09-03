(ns upgrade.twitchbot
  (:require [clojure.core.async :as async]
            [instaparse.core :as insta]
            [org.httpkit.server :refer [send!]]
            [cognitect.transit :as transit]
            [upgrade.common :refer [log transitWrite]]
            [upgrade.freesound :refer [search-and-play-nth!
                                       play-sound!
                                       players-stop!
                                       play-not-found!
                                       search-and-play-file!
                                       play-mp3-from-url!]]
             [upgrade.messages :refer [chatbot-help-message
                                       github-message
                                       play-reply
                                       play-help-message
                                       shout-out-message
                                       today-message
                                       welcome-message]]
             [upgrade.twitch :refer [getEmoteChangeSet!
                                     emoteSetRegexStr
                                     findEmoteImageUrl]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [net.engio.mbassy.listener Handler]
           [org.kitteh.irc.client.library Client]
           [org.kitteh.irc.client.library.event.channel
            ChannelMessageEvent
            ChannelNoticeEvent
            ChannelJoinEvent
            ChannelPartEvent]
           [org.kitteh.irc.client.library.event.connection
            ClientConnectionFailedEvent
            ClientConnectionEstablishedEvent]
           [org.kitteh.irc.client.library.event.client
            ClientReceiveCommandEvent
            ClientReceiveNumericEvent]
           [org.kitteh.irc.client.library.feature.twitch TwitchSupport]
           ))

;; TODO: move into a single atom that stores all the internal state of this
;; chatbot
(defonce ws-clients (atom #{}))
(defonce twitchbot-state (atom {}))

(def chatbot-command-list ["play" "stop" "help"
                           "so" "today" "welcome" "github"])

(defn command-parser []
  (str "cmd = "
       (apply str (interpose " | " chatbot-command-list )) "\n"
      
       "help = <\"!help\"> | <\"!help\"> <space> "
       (str "(" (apply str (interpose " | " (map (fn [cmd] (str "\"" cmd  "\""))
                                                 chatbot-command-list))) ")") " \n"

       "play = <\"!play\"> <space> sound-search\n"
       "sound-search = first-result-search | nth-result-search | sound-id \n"
       "first-result-search = #\".+\"\n"
       "nth-result-search = #\"\\d+\" <space> first-result-search\n"
       "sound-id = #\"\\d+\"\n"
       
       "stop = <\"!stop\">\n"
       "so = <\"!so\"> <space> username | <\"!so\"> <space> <\"@\">username\n"

       "today = <\"!today\">\n"

       "welcome = <\"!welcome\"> | <\"!welcome\"> <space> <\"@\">username\n"

       "github = <\"!github\"> "
       
       "username= #\"[^\\s]+\"\n"
       "space= #\"\\s+\"\n"
       ))

(defn handle-channel-command [evt]
  (let [msg (. evt getMessage)
        ;; server-msg (. (. evt getSource) (getMessage))
        cmd-parser (insta/parser (command-parser))
        parse-result (cmd-parser msg)
        parse-failure? (insta/failure? parse-result)]

    (if parse-failure?

      ;; handle failure
      (let [actor (. evt getActor)
            nick (.getNick actor)]
        ;;(log (str "Chatbot actor: " (. evt getActor)))
        ;;(log (str "Chatbot source: " (. evt getSource)))
        (log (str "Chatbot nick: " nick))
        (log (str "Chatbot heard: " msg))
        
        (doseq [ch @ws-clients]
          (send! ch (transitWrite {:animation-key :chat
                                   :payload {:msg msg :nick nick}})))
        ;;(.sendReply evt (str "I heard ya! But that's not a command: " msg ))
        )
      
      ;; otherwise, do command
      (let  [[_ [command args]] parse-result] 
        (cond

          (= command :help)
          (cond
            (= args "play")
            (.sendReply evt (play-help-message))
            :else
            (.sendReply evt (chatbot-help-message chatbot-command-list))

            )
          
          (= command :so)
          (if-let [[_ username] args]
            (do
              (println "Shout Out to " username)
              (.sendReply evt (shout-out-message))))

          (= command :today)
          (.sendReply evt (today-message))

          (= command :welcome)
          (if args
            (.sendReply evt (welcome-message (str "@" (second args))))
            (.sendReply evt (welcome-message)))

          (= command :github)
          (.sendReply evt (github-message))

          (= command :play)
          (let [[_ [search-type]] args]
            (case search-type

              :first-result-search
              (let [[_ [_ search-term]] args]

                (if-let [mp3-result (search-and-play-file! search-term)]
                  ;; if the search-term matches an mp3 file inside the
                  ;; mp3 directory, then play it.
                  (.sendReply
                   evt
                   (play-reply
                    (str "Found local '" search-term ".mp3' file!!!")))

                  ;; Check if this is a url and try playing that
                  (if (re-matches #"http.+\.mp3" search-term)

                    ;; this is a mp3 url
                    (do
                      (play-mp3-from-url! search-term)
                      (.sendReply
                       evt
                       (play-reply
                        (str "Successfully loaded mp3 from url '"
                             search-term "'!!!"))))

                  ;; otherwise, this is a freesound.org search
                    (let [url (search-and-play-nth! search-term 0)]
                      (if url 
                        (.sendReply evt (play-reply url))
                        ;; else
                        (play-not-found!)))                    
                    )))
              
              :nth-result-search
              (let [[_ [_ idx [_ search-term]]] args
                    url (search-and-play-nth! search-term (Integer/parseInt idx))]
                (if url
                  (.sendReply evt (play-reply url))
                  ;; else 
                  (play-not-found!)))

              :sound-id
              (let [[_ [_ sound-id]] args]
                (if-let [url (play-sound! sound-id)]
                  (.sendReply evt (play-reply url))
                  ;; else
                  (play-not-found!)))

              (play-not-found!)))

          (= command :stop)
          (players-stop!)
          
          :else
          (log (str "That's a valid command, but it's not implemented yet:" msg))

          )))))

(defn handle-channel-message
  "Parse messages for things like emotes. commands are handled by a
  different fn"
  [evt {:keys [clientid]}]
  (let [msg (. evt getMessage)
        ;; we call twitch api to build a list of possible emotes
        emote-change-set (getEmoteChangeSet! clientid 0)
        matcher (re-matcher
                 (re-pattern (emoteSetRegexStr emote-change-set))
                 msg)]
    (if-let [found (re-find matcher)]
      (doseq [ch @ws-clients]
        (let [url (findEmoteImageUrl emote-change-set found)]
          (send! ch (transitWrite {:animation-key :emoticons
                                   :url url})))))
    ))

(defn handle-event [evt opts]
  "Here's the main event handler. This is where we can implement fun stuff"
  (let [event-type (type evt)
        ;; Seems like class is same as type
        ;; event-class (class evt)
        ]
    
    (cond

      ;; Do stuff when a message was sent to the channel
      (instance? ChannelMessageEvent evt)
      (do
        ;; look for commands like !play
        (handle-channel-command evt)
        ;; look for things like emotes
        (handle-channel-message evt opts))

      (instance? ChannelJoinEvent evt)
      (let [username (.getUser evt)
            nick (.getNick username)
            client (.getClient evt)]
        ;; (log (str nick "just joined!!"))
        ;; This actually fires for anyone (even if they don't chat)
        ;; So, not sure we should do anything because some people like to lurk
        ;; This is working. Next step is to use this information about
        ;; when people join in a fun way
        ;; (send-message
        ;;   client
        ;;   channel
        ;;   (str "Welcome, " nick
        ;;        ", to the stream! Please introduce "
        ;;        "yourself and tell us why you're interested in clojure."))
        )

      (instance? ChannelPartEvent evt)
      (let [username (.getUser evt)
            nick (.getNick username)
            client (.getClient evt)]
        ;;(log (str nick " just left!!"))
        )

      (instance? ClientReceiveCommandEvent evt)
      (let []
        ;;(log (str "ClientRecieveCommandEvent"))
        ;;(log evt)
        )

      (instance? ClientConnectionFailedEvent evt)
      (let []
        (log (str "Got Disconnected??!!"))
        (log (str evt)))

      ;; TODO I was thinking this might be useful to reconnect after
      ;; network error, but now I don't think we really need to do
      ;; anything for these types of events
      
      ;; (instance? ClientConnectionEstablishedEvent)
      ;; (let []
      ;;   )
      
      :else
      (log (str "NEED IMPLEMENTATION FOR: " event-type))
      
      )))

;; Make a class with methods for handling events
(defprotocol IrcListeners

  "Methods for responding to irc events"
  (^{Handler true} listen-for-all-events [this evt] "Listen for any event"))

(deftype IrcEventHanders [opts]
  IrcListeners
  (^{Handler true}   
   listen-for-all-events [this evt] (handle-event evt opts)))

(defn add-listeners [client opts]
  (let [evt-mgr (. client (getEventManager))]
    (. evt-mgr (registerEventListener (IrcEventHanders. opts)))))

(defn send-message
  [client channel message]
  (. client (sendMessage channel message)))

(defn leave-and-disconnect
  [client channel]
  (. client (removeChannel channel "Later alligators"))
  (. client (shutdown "UpgradingChatBot is shutting down")))

(defn send-message-in-future
  [client channel message millis]
  (async/go
    (let [_ (async/alts! [(async/timeout millis)])]
      (send-message client channel message))))

(defn schedule-repeating-messages
  "This will return function that can be used to stop the repeating
  messages"
  [client channel messages millis]
  (let [continue-repeating-messages? (atom true)]
    (async/go
      (loop [t (async/timeout millis)]
        (let [_ (async/alts! [t])]
          (if @continue-repeating-messages?
            (do
              (doseq [m messages] (send-message client channel m))
              (doseq [ch @ws-clients
                      m messages]
                (send! ch (transitWrite {:animation-key :banner-message
                                         :payload {:msg m}})))
              
              (recur (async/timeout millis)))))))

    (swap! twitchbot-state update-in [:stop-fns]
           (fn [cur]
             (let [stop-fn (fn []
                             (reset! continue-repeating-messages? false))
                   existing (:stop-fns cur)]
               (conj existing stop-fn ))))))

(defn create-chatbot!
  "Creates an chatbot client object that can be used later to connect
  and listen to channels"
  [host port username oauth]
  (let [client (.. (Client/builder)
                   (server)
                   (host host)
                   (port port)
                   (password oauth)
                   (then)
                   (nick username)
                   (build))]

    ;; Add Twitch support
    (TwitchSupport/addSupport client)))

(defn connect-and-add-channel!
  "Connect and start listening to channels"
  [client channel opts]

  (add-listeners client opts)

  (. client (connect))
  (. client addChannel (into-array String [channel]))
  
  client)

(defn start-twitchbot!
  [{:keys [twitchbot twitchapi] :as system}]
  (let [{:keys [host port username oauth channel]} twitchbot
        {:keys [clientid]} twitchapi
        client (create-chatbot! host port username oauth)
        ;; oauth is no longer needed in memory, so let's clear it
        twitchbot (assoc twitchbot
                         :oauth nil
                         :client (connect-and-add-channel! client channel
                                                           {:clientid clientid})
                         :running? true)]
    
    ;; now we have a fully started twichbot
    (send-message client channel "UpgradingChatBot is ALIVE")

    ;; setup scheduled messages
    ;; TODO: implement the ability to stagger these (with offset)
    ;;600000 ;; every 10 minutes
    ;;900000 ;; every 15 minutes
    ;;1200000 ;; every 20 minutes
    ;;2100000 ;; every 35 minutes

    (schedule-repeating-messages client channel
                                 [(welcome-message)] 1200000)

    (schedule-repeating-messages client channel
                                 [(today-message)] 900000)

    (schedule-repeating-messages client channel
                                 [(github-message)] 21000000)

    twitchbot))

(defn stop-twitchbot! [{:keys [twitchbot] :as system}]
  (let [{:keys [client channel]} twitchbot
        stop-functions (:stop-fns @twitchbot-state)]
    (log "Attempting to stop twitch chat bot ...")
    (log client)
    (log channel)
    (leave-and-disconnect client channel)
    (doseq [stop-fn stop-functions]
      (stop-fn))))
