(ns upgrade.messages)

(defn github-message []
  (str "Checkout my github repository for the source code for the "
       "upgradingchatbot: https://github.com/upgradingdave/upgradingchatbot"))

(defn today-message []
  (str "Today I'll be adding more basic functionality to my text editor. Currently, I can kinda, sorta enter \"lines\" of text. It'd be nice to be able to backspace and delete as well!"))

(defn chatbot-help-message [chatbot-command-list]
  (str
   "B) The UpgradingChatBot is online and here to help! "
   "Have fun! Here are the commands: "
   (apply str (interpose ", " (map (fn [command] (str "!" command))
                                   chatbot-command-list)))
   ". Try `!help <command>` for more details on each command."))

(defn welcome-message
  ([]
   (str
    "HeyGuys "
    "Welcome! "
    "Since April 2019, I'm on a challenge to program on stream every week for a year. "
    "My goal is become a better programmer by exploring my favorite programming "
    "language Clojure and meet other programmers. "
    "If you're interested in clojure here's a great site to get started: "
    "https://www.braveclojure.com/"))
  ([username]
   (str
    "HeyGuys "
    "Welcome, " username "! Thanks for stopping by! Please feel free to say \"hi\" in the chat and let me know your favorite programming language and what you're working on"
    "Type !help in the chat for a list of commands and please try them out!")))

(defn play-reply [url]
  (str "SingsNote SingsNote SingsNote " url))

(defn play-help-message []
  (str "You can play mp3 sounds from the chat! For example, give this a shot: `!play sipping coffee`. The !play command will search https://freesound.org and will play the first result of the search. In order to play the second search result, for example, you can pass a search index like this: `!play 1 sipping coffee`."))

(defn shout-out-message [username]
  (str "Shout out to https://www.twitch.tv/"
       username
       " Go and check out their stream!")) 
