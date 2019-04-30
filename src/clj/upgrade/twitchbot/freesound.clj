(ns upgrade.twitchbot.freesound
  (:require [upgrade.twitchbot.common :refer [decrypt]]
            [clojure.java.io :as io]
            [clj-http.client :as client]))

;; mp3 player state

(def players (atom '()))

(defn player-alive? [player]
  (and (not (nil? player)) (.isAlive player)))

(defn players-cleanup []
  (letfn [(f [col] (filter (fn [n] (player-alive? n)) col))]
    (swap! players f)))

(defn players-stop []
  (doseq [player @players]
    (if (player-alive? player)
      (.stop player)))
  (players-cleanup))

(defonce api-key 
  (decrypt "AAAADH2MEPeapWrwrhmAOKGL+hglh++LIwGUoydlUrUm4H7RDtfw+ztNc8IEkMM3aFZawUBdHnJr45pW0qXAzhu9XddLXns/"))

(defn search [search-term]
  (let [url (str "https://freesound.org/apiv2/search/text/?query=" search-term)]
    (client/get (str url "&token=" api-key) {:as :json})))

(defn first-search-result [search-response]
  (first (:results (:body search-response))))

(defn nth-search-result [search-response n]
  (nth (:results (:body search-response)) n))

(defn sound [sound-id]
  (let [url (str "https://freesound.org/apiv2/sounds/" sound-id "/" "?token=" api-key)
        response (client/get url {:as :json})]
    response))

(defn get-preview-hq-mp3-url [sound-response]
  (:preview-hq-mp3 (:previews (:body sound-response))))

(defn fetch-mp3 [sound-id]
  "Make request for preview hq mp3 and return byte array stream"
  (let [sound-response (sound sound-id)
        url (get-preview-hq-mp3-url sound-response)
        res (client/get url {:as :byte-array})]
    (if (= (:status res) 200)
      (:body res))))

(defn play-input-stream [fis]
  (let [bis (java.io.BufferedInputStream. fis)
        player (javazoom.jl.player.Player. bis)
        t (Thread. #(doto player (.play) (.close)))]
    (swap! players conj t)
    (.start t)))

(defn play-file [filename & opts]
  (let [fis (java.io.FileInputStream. filename)]
    (play-input-stream fis opts)))

(defn fetch-mp3-to-file [sound-id]
  (let [stream (fetch-mp3 sound-id)]
    (if (not (nil? stream))
      (with-open [w (io/output-stream (str sound-id ".mp3"))]
        (.write w stream)))))

(defn fetch-mp3-and-play [sound-id]
  (let [stream (fetch-mp3 sound-id)]
    (if (not (nil? stream))
      (let [bis (java.io.ByteArrayInputStream. stream)]
        (play-input-stream bis)))))

(defn search-and-save-first [search-term]
  (if-let [sound-id (:id (first-search-result (search search-term)))]
    (fetch-mp3-to-file sound-id)))

(defn search-and-play-first [search-term]
  (if-let [sound-id (:id (first-search-result (search search-term)))]
    (fetch-mp3-and-play sound-id)))

(defn search-and-play-nth [search-term n]
  (if-let [sound-id (:id (nth-search-result (search search-term) n))]
    (fetch-mp3-and-play sound-id)))

