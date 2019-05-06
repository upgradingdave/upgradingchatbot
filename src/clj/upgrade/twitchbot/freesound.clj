(ns upgrade.twitchbot.freesound
  (:require [upgrade.twitchbot.common :refer [decrypt]]
            [clojure.java.io :as io]
            [clj-http.client :as client]))

;; methods to manage mp3 player state

(def players (atom '()))

(defn player-alive? [player]
  (and (not (nil? player)) (.isAlive player)))

(defn players-cleanup! []
  (letfn [(f [col] (filter (fn [n] (player-alive? n)) col))]
    (swap! players f)))

(defn players-stop! []
  (doseq [player @players]
    (if (player-alive? player)
      (.stop player)))
  (players-cleanup!))

;; methods to request info from the rest api

(defonce api-key 
  (decrypt "AAAADH2MEPeapWrwrhmAOKGL+hglh++LIwGUoydlUrUm4H7RDtfw+ztNc8IEkMM3aFZawUBdHnJr45pW0qXAzhu9XddLXns/"))

(defn fetch-search-results! [search-term]
  (let [url (str "https://freesound.org/apiv2/search/text/?query=" search-term)]
    (client/get (str url "&token=" api-key) {:as :json})))

(defn fetch-sound! [sound-id]
  (let [url (str "https://freesound.org/apiv2/sounds/" sound-id "/" "?token=" api-key)
        response (client/get url {:as :json})]
    (:body response)))

(defn fetch-mp3! [mp3-url]
  "Make request for mp3 and return byte array stream"
  (let [res (client/get mp3-url {:as :byte-array})]
    (if (= (:status res) 200)
      (:body res))))

(defn first-search-result [search-response]
  (first (:results (:body search-response))))

(defn nth-search-result [search-response n]
  (nth (:results (:body search-response)) n nil))

;; functions to play audio
;; most of these functions expect the result from a fetch-sound call

(defn play-input-stream! [fis]
  (let [bis (java.io.BufferedInputStream. fis)
        player (javazoom.jl.player.Player. bis)
        t (Thread. #(doto player (.play) (.close)))]
    (swap! players conj t)
    (.start t)
    t))

(defn play-file! [filename & opts]
  (let [fis (java.io.FileInputStream. filename)]
    (play-input-stream! fis opts)))

(defn fetch-mp3-to-file! [sound-result]
  (let [sound-id (:id sound-result)
        mp3-url (:preview-hq-mp3 (:previews sound-result))  
        stream (fetch-mp3! mp3-url)]
    (if (not (nil? stream))
      (with-open [w (io/output-stream (str sound-id ".mp3"))]
        (.write w stream)))))

(defn fetch-mp3-and-play! [sound-result]
  (let [sound-id (:id sound-result)
        mp3-url (:preview-hq-mp3 (:previews sound-result))  
        stream (fetch-mp3! mp3-url)]
    (if (not (nil? stream))
      (let [bis (java.io.ByteArrayInputStream. stream)]
        (play-input-stream! bis)
        sound-result))))

;; convenience functions

(defn search-and-save-first! [search-term]
  (if-let [search-result (first-search-result (fetch-search-results! search-term))]
    (fetch-mp3-to-file! (fetch-sound! (:id search-result)))))

(defn search-and-play-first! [search-term]  
  (if-let [search-result (first-search-result (fetch-search-results! search-term))]
    (fetch-mp3-and-play! (fetch-sound! (:id search-result)))))

(defn search-and-play-nth! [search-term n]
  (if-let [search-result (nth-search-result (fetch-search-results! search-term) n)]
    (fetch-mp3-and-play! (fetch-sound! (:id search-result)))))

(defn play-not-found! []
  (fetch-mp3-and-play! (fetch-sound! 216090)))
