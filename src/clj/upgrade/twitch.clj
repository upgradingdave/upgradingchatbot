(ns upgrade.twitch
  (:require [cheshire.core :refer [parse-string generate-string]]
            [org.httpkit.client :as http]
            [upgrade.common :refer [get-config
                                    log
                                    routes
                                    twitch-followers-callback-url]]))

;; This doesn't work anymore I guess?
;;https://api.twitch.tv/kraken/chat/emoticons&client_id=xxx
;;https://api.twitch.tv/v5/chat/emoticons&client_id=xxx
;; https://api.twitch.tv/kraken/chat/emoticon_images?emotesets=46&client_id=xxx

;; better way to find emoticons??
;;https://twitchemotes.com/api_cache/v3/subscriber.json

(defn getEmoteChangeSetLookup []
  (let [options (:timeout 200)
        url (str "https://twitchemotes.com/api_cache/v3/subscriber.json")
        {:keys [status headers body error]} @(http/get url)]
    (if error
      {:error error}
      ;; on success
      (let [result (parse-string body true)]
        result))))

(defn getEmotesForChannel [clientid channel-id]
  ;;GET https://api.twitch.tv/kraken/chat/<channel ID>/badges
  (let [options (:timeout 200)
        url (str "https://api.twitch.tv/kraken/chat/" channel-id "/badges"
                 "?client_id=" clientid)
        {:keys [status headers body error]} @(http/get url)]
    (if error
      {:error error}
      ;; on success
      (let [result (parse-string body true)]
        result))))

(defn getEmoteChangeSet!
  "Makes a http GET request to the twitch v5 api to get code's and id's
  of emotes inside a emote set. Returns a list of maps of :id
  and :code"
  [clientid emotesetid]
  (let [options (:timeout 200)
        url (str "https://api.twitch.tv/kraken" "/chat/emoticon_images"
                 "?emotesets=" emotesetid
                 ;; 20191017 for some reason, passing clientid stopped
                 ;;working??
                 ;; "&client_id=" clientid
                 )
        {:keys [status headers body error]} @(http/get url)]
    ;;(log url)
    (if error
      {:error error}
      ;; on success
      (let [result (parse-string body true)]
        result
        (get-in result [:emoticon_sets (keyword (str emotesetid))])))))

(defn searchEmoteChangeSet
  "Search thru the results of `getEmoteChangeSet!` for a specific
  emote. For example `(searchEmoteChangeSet emote-change-set \"HeyGuys\")`"  
  [emote-change-set emote-code]
  (first (filter #(= emote-code (:code %)) emote-change-set)))

(defn findEmoteImageUrl
  "Search thru results of `getEmotesChangeSet!` for a emote-code and
  then build image url for that emote"
  ([emote-change-set emote-code]
   (findEmoteImageUrl emote-change-set emote-code "3.0"))
  ;;Valid size can be 0.0 - 3.0
  ([emote-change-set emote-code size]
   (let [result (searchEmoteChangeSet emote-change-set emote-code)]
     (str "https://static-cdn.jtvnw.net/emoticons/v1/"
          (:id result) "/" size ))))

(defn emoteSetRegexStr
  "Return a regex that can be used to see if string contains emotes"
  [emote-change-set]
  (apply str (interpose
              "|"
              (map (fn [n] (java.util.regex.Pattern/quote (:code n)))
                   emote-change-set))))

(defn get-app-access-token
  "Do the oauth handshake to get an app access token. This retuns a
  datastructure like this:

  {:access_token \"xxx\",
   :expires_in 5685328,
   :token_type \"bearer\"}

  "
  [twitch-client-id twitch-client-secret]
  (let [options {}
        url (str "https://id.twitch.tv/oauth2/token"
                 "?client_id=" twitch-client-id
                 "&client_secret=" twitch-client-secret
                 "&grant_type=client_credentials"
                 ;; "&scope=analytics:read:extensions+analytics:read:games"
                 ;; "+bits:read+channel:read:subscriptions+clips:edit"
                 ;; "+user:edit+user:edit:broadcast+user:read:broadcast"
                 ;; "+user:read:email"
                 ) 
        {:keys [status headers body error] :as response} @(http/post url options)]
    (let [result (parse-string body true)]
      result)))

(defn get-twitch-user-info
  "Get information about a twitch user"
  [twitch-client-id twitch-username]
  (let [options {:timeout 200 ;;ms
                 :headers {"Client-ID" twitch-client-id
                           "Content-Type" "application/json"}}
        url (str "https://api.twitch.tv/helix/users?login=" twitch-username) 
        {:keys [status headers body error]} @(http/get url options)]
    (let [result (parse-string body true)]
      (first
       (:data result)))))

(defn get-twitch-followers
  "returns data structure that contains `{:total <total follows>, :data <list>}`"
  [twitch-client-id followed-id]
  (let [options {:timeout 200 ;;ms
                 :headers {"Client-ID" twitch-client-id
                           "Content-Type" "application/json"}}
        url (str  "https://api.twitch.tv/helix/users/follows?from_id=" followed-id) 
        {:keys [status headers body error]} @(http/get url options)]
    (let [result (parse-string body true)]
      result)))

(defn get-webhook-subscriptions
  "Returns information about which webhooks I'm currently subscribing to"
  [twitch-client-id twitch-app-token]
  (let [options {:timeout 200 ;;ms
                 :headers {"Client-ID" twitch-client-id
                           "Authorization" (str "Bearer " twitch-app-token)
                           "Content-Type" "application/json"}}
        url (str  "https://api.twitch.tv/helix/webhooks/subscriptions") 
        {:keys [status headers body error]} @(http/get url options)]
    (let [result (parse-string body true)]
      result)))

(defn follower-subscription-topic-url [to-id]
  (str "https://api.twitch.tv/helix/users/follows?"
       "first=1&to_id=" to-id))

(defn follows-webhook
  "This function subscribes (or unsubscribes) so that each time a twitch
  user with `to-id` is the followed, Twitch will send a GET request to
  the `callback-url`.  `twitch-app-client-id` is the id you get when
  you create a Twitch App."
  [twitch-client-id to-id callback-url seconds subscribe?]
  (let [hub-topic (follower-subscription-topic-url to-id)
        req-body (generate-string
                  {:hub.callback callback-url
                   :hub.mode (if subscribe? "subscribe" "unsubscribe")
                   :hub.topic hub-topic
                   :hub.lease_seconds seconds
                   ;; TODO pass hub.secret
                   ;;:hub.secret ""
                   })
        options {:timeout 200 ;;ms
                 :headers {"Client-ID" twitch-client-id
                           "Content-Type" "application/json"}
                 :body req-body}
        url "https://api.twitch.tv/helix/webhooks/hub"

        result @(http/post url options)
        
        {:keys [status headers body error]} result]
    
    (log (str "TWITCH WEBHOOK FOLLOWS: " req-body))
    (log (str "TWITCH WEBHOOK FOLLOWS: " status))
    ;; (if (and (> status 200) (< status 300))
    ;;   (log (str "Successfully requested Subscription: " body) )
    ;;   (do
    ;;     (log "Hmm, error when attempting subscription")
    ;;     (log result)))
    ))

(defn active-follower-subscription?
  [follow-user-id
   followers-callback-url
   get-webook-subscriptions-result]
  (let [{:keys [total data]} get-webook-subscriptions-result]
    (not (empty? (filter
                  (fn [{:keys [topic callback]}]
                    (and
                     (= callback followers-callback-url)
                     (= (follower-subscription-topic-url follow-user-id)
                        topic))) data)))))

(defn subscribe-to-follows [twitch-client-id to-id callback-url seconds]
  (follows-webhook twitch-client-id to-id callback-url seconds true))

(defn unsubscribe-to-follows [twitch-client-id to-id callback-url seconds]
  (follows-webhook twitch-client-id to-id callback-url seconds false))

(comment
  (def conf (get-config))
  (def clientid (get-in conf [:twitchapi :clientid]))
  (def client-secret (get-in conf [:twitchapi :client-secret]))
  (def app-token (get-in conf [:twitchapi :app-token-results :access_token]))
  (def follow-user-id (get-in conf [:twitchapi :follow-user-id]))
  (def followers-callback-url (get-in conf [:twitchapi :followers-callback-url]))
  (getEmoteChangeSet clientid 0))
