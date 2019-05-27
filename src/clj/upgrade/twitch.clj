(ns upgrade.twitch
  (:require [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]
            [upgrade.common :refer [get-config]]
            ))

(def default-clientid (get-in (get-config) [:twitchapi :clientid]))
(def twitch-api-url "https://api.twitch.tv/kraken")
(def twitch-emote-url "https://static-cdn.jtvnw.net/emoticons/v1")

;; This doesn't work anymore I guess?
;;https://api.twitch.tv/kraken/chat/emoticons&client_id=xxx
;;https://api.twitch.tv/v5/chat/emoticons&client_id=xxx

;; https://api.twitch.tv/kraken/chat/emoticon_images?emotesets=46&client_id=xxx
(defn getEmoteChangeSet!
  "Makes a http GET request to the twitch v5 api to get code's and
  id's of emotes inside a emote set. Returns a list of maps of :id and :code"
  [clientid emotesetid]
  (let [options (:timeout 200)
        url (str twitch-api-url "/chat/emoticon_images"
                 "?emotesets=" emotesetid
                 "&client_id=" clientid)
        {:keys [status headers body error]} @(http/get url)]
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
     (str twitch-emote-url "/" (:id result) "/" size ))))

;;(re-find (re-pattern (emoteSetRegex (drop 100 set0))) "This is a HeyGuys")
(defn emoteSetRegexStr
  "Return a regex that can be used to see if string contains emotes"
  [emote-change-set]
  (apply str (interpose
              "|"
              (map (fn [n] (java.util.regex.Pattern/quote (:code n)))
                   emote-change-set))))

(comment
  (def conf (get-config))
  (def clientid (get-in conf [:twitchapi :clientid]))
  (getEmoteChangeSet clientid 0))
