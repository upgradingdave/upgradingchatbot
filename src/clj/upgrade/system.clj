(ns upgrade.system
  (:require [com.stuartsierra.component :as component]
            [upgrade.common :refer [log get-config]]
            [upgrade.twitchbot :as bot]))

;; TODO: logger might be a separate component?

(defrecord TwitchChatBot [host port username oauth channel client]
  component/Lifecycle

  (start [twitchbot]
    (log "Attempting to start twitch chat bot ...")
    (let [twitchbot (assoc twitchbot
                           :host host
                           :port port
                           :username username
                           :oauth oauth
                           :channel channel)
          client (bot/create-chatbot host port username oauth)
          ;; oauth is no longer needed, so let's get rid of it
          twitchbot (assoc twitchbot :oauth nil)
          twitchbot (assoc twitchbot :client (bot/connect-and-add-channel client channel))]

      ;; now we have a fully started twichbot
      (bot/send-message twitchbot "UpgradingChatBot is ALIVE")
      (bot/schedule-repeating-messages
       twitchbot
       600000 ;; every 10 minutes
       [(bot/welcome-message)
        (bot/chatbot-help-message)
        (bot/today-message)])

      twitchbot
      ))

  (stop [twitchbot]
    (log "Attempting to stop twitch chat bot ...")
    (bot/leave-and-disconnect twitchbot)
    ;; TODO should I set all fields to nil? like host, username, etc?
    ;; TODO should I set a boolean running? to false?
    (assoc twitchbot :client nil)))

(defn new-twitchbot []
  (map->TwitchChatBot (:twitchbot (get-config))))

(defn system []
  (component/system-map
   :twitchbot (new-twitchbot)))

(comment
  (def s (component/start (system))))
