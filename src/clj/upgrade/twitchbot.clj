(ns upgrade.twitchbot
  (:require [clojure.core.async :as async]
            [instaparse.core :as insta]
            [upgrade.common :refer [log]]
            [upgrade.freesound :refer [search-and-play-nth!
                                       play-sound!
                                       players-stop!
                                       play-not-found!]])
  (:import [net.engio.mbassy.listener Handler]
           [org.kitteh.irc.client.library Client]
           [org.kitteh.irc.client.library.event.channel
            ChannelMessageEvent
            ChannelNoticeEvent
            ChannelJoinEvent
            ChannelPartEvent]
           [org.kitteh.irc.client.library.event.client
            ClientReceiveCommandEvent
            ClientReceiveNumericEvent]
           [org.kitteh.irc.client.library.feature.twitch TwitchSupport]
           ))

(def chatbot-command-list ["play" "stop" "help" "so" "today" "welcome"])

;; Common Messages

;; "I love when people try out the chatbot. Fine more info here: "
;;   "https://github.com/upgradingdave/upgradingchatbot "

(defn today-message []
  (str "Today, we're working on adding chat commands and maybe get back into building a Twitch Extension!"))

(defn chatbot-help-message []
  (str
   "B) The UpgradingChatBot is online and here to help! "
   "Have fun! Here are the commands: " (apply str (interpose ", " (map (fn [command] (str "!" command)) chatbot-command-list))) ". Try `!help <command>` for more details on each command."))

(defn welcome-message
  ([]
   (str
    "HeyGuys "
    "Welcome! "
    "Since April 2019, I'm on a challenge to live stream 3 times a week for a year. "
    "My goal is become a better programmer by exploring my favorite programming "
    "language, Clojure and meeting other programmers like you. "
    "If you're interested in clojure here's a great site to get started: "
    "https://www.braveclojure.com/"))
  ([username]
   (str
    "HeyGuys "
    "Welcome, " username "! Great to have you here, grab a frosty beverage and "
    "help us write some clojure. If you have suggestions or questions, please don't "
    "be shy. Type !help in the chat for a list of commands and please try them out!")))

(defn command-parser []
  (str "cmd = " (apply str (interpose " | " chatbot-command-list)) "\n"
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
       
       "username= #\"[^\\s]+\"\n"
       "space= #\"\\s+\"\n"
       ))

(defn freesound-reply [url]
  (str "SingsNote SingsNote SingsNote " url))

(defn handle-channel-message [evt]
  (let [msg (. evt getMessage)
        ;; server-msg (. (. evt getSource) (getMessage))
        cmd-parser (insta/parser (command-parser))
        parse-result (cmd-parser msg)
        parse-failure? (insta/failure? parse-result)]

    (if parse-failure?

      ;; handle failure
      (do
        (log (str "Chatbot heard: " msg))
        ;;(.sendReply evt (str "I heard ya! But that's not a command: " msg ))
        )
      
      ;; otherwise, do command
      (let  [[_ [command args]] parse-result] 
        (cond

          (= command :help)
          (cond
            (= args "play")
            (.sendReply evt (str "You can play mp3 sounds from the chat! For example, give this a shot: `!play sipping coffee`. The !play command will search https://freesound.org and will play the first result of the search. In order to play the second search result, for example, you can pass a search index like this: `!play 1 sipping coffee`."))

            :else
            (.sendReply evt (chatbot-help-message))

            )
          
          (= command :so)
          (if-let [[_ username] args]
            (do
              (println "Shout Out to " username)
              (.sendReply evt (str "Shout out to https://www.twitch.tv/"
                                   username
                                   " Go and check out their stream!"))))

          (= command :today)
          (.sendReply evt (today-message))

          (= command :welcome)
          (if args
            (.sendReply evt (welcome-message (str "@" (second args))))
            (.sendReply evt (welcome-message)))

          (= command :play)
          (let [[_ [search-type]] args]
            (case search-type

              :first-result-search
              (let [[_ [_ search-term]] args
                    url (search-and-play-nth! search-term 0)]
                (if url 
                  (.sendReply evt (freesound-reply url))
                  ;; else
                  (play-not-found!)))
              
              :nth-result-search
              (let [[_ [_ idx [_ search-term]]] args
                    url (search-and-play-nth! search-term (Integer/parseInt idx))]
                (if url
                  (.sendReply evt (freesound-reply url))
                  ;; else 
                  (play-not-found!)))

              :sound-id
              (let [[_ [_ sound-id]] args]
                (if-let [url (play-sound! sound-id)]
                  (.sendReply evt (freesound-reply url))
                  ;; else
                  (play-not-found!)))

              (play-not-found!)))

          (= command :stop)
          (players-stop!)
          
          :else
          (log (str "That's a valid command, but it's not implemented yet:" msg))

          )))))

(defn handle-event [evt]
  "Here's the main event handler. This is where we can implement fun stuff"
  (let [event-type (type evt)
        ;; Seems like class is same as type
        ;; event-class (class evt)
        ]
    
    (cond

      ;; Do stuff when a message was sent to the channel
      (instance? ChannelMessageEvent evt)
      (handle-channel-message evt)

      (instance? ChannelJoinEvent evt)
      (let [username (.getUser evt)
            nick (.getNick username)
            client (.getClient evt)]
        (log (str nick "just joined!!"))
        ;; This is working. Next step is to use this information about when people join
        ;; in a fun way
        ;; (send-message client
        ;;               (str "Welcome, " nick
        ;;                    ", to the stream! Please introduce "
        ;;                    "yourself and tell us why you're interested in clojure."))
        )

      (instance? ChannelPartEvent evt)
      (let [username (.getUser evt)
            nick (.getNick username)
            client (.getClient evt)]
        (log (str nick " just left!!")))

      (instance? ClientReceiveCommandEvent evt)
      (log (str "Received: ClientRecieveCommandEvent"))
      
      :else
      (log (str "NEED IMPLEMENTATION FOR: " event-type))
      
      )))

;; Make a class with methods for handling events
(defprotocol IrcListeners

  "Methods for responding to irc events"
  (^{Handler true} listen-for-all-events [this evt] "Listen for any event"))

(deftype IrcEventHanders []
  IrcListeners
  (^{Handler true}   
   listen-for-all-events [this evt] (handle-event evt)))

(defn add-listeners [client]
  (let [evt-mgr (. client (getEventManager))]
    (. evt-mgr (registerEventListener (IrcEventHanders.)))))

(defn send-message
  ([{client :client channel :channel :as twitchbot} message]
   (. client (sendMessage channel message))))

(defn leave-and-disconnect
  [{client :client channel :channel :as twitchbot}]
  (. client (removeChannel channel "Later alligators"))
  (. client (shutdown "UpgradingChatBot is shutting down")))

(defn send-message-in-future
  [twitchbot client millis message]
  (async/go
    (let [_ (async/alts! [(async/timeout millis)])]
      (send-message twitchbot message))))

;; TODO Do we make this part of the component?
(defonce continue-repeating-messages? (atom true))

(defn schedule-repeating-messages
  [{client :client :as twitchbot} millis messages]
  (reset! continue-repeating-messages? true)
  (async/go
    (loop [t (async/timeout millis)]
      (let [_ (async/alts! [t])]
        (if @continue-repeating-messages?
          (do
            (doseq [m messages]
              (send-message twitchbot m))
            (recur (async/timeout millis))))))))

(defn stop-messages []
  (reset! continue-repeating-messages? false))

(defn create-chatbot [host port username oauth]
  "Creates an chatbot client object that can be used later to connect
  and listen to channels"
  (let [client (.. (Client/builder)
                   (server)
                   (host host)
                   (port port)
                   (password oauth)
                   (then)
                   (nick username)
                   (build))]
    ;; Not sure how to handle exceptions
    ;;(.getExceptionListener client)
    (TwitchSupport/addSupport client)))

(defn connect-and-add-channel [client channel]
  "Connect and start listening to channels"
  (add-listeners client)

  (. client (connect))
  (. client addChannel (into-array String [channel]))
  
  client)

