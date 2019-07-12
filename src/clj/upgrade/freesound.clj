(ns upgrade.freesound
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [upgrade.common :refer [get-config decrypt]]))

(defn md5 [^String s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (java.math.BigInteger. 1 raw))))

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
  (let [conf (get-config)]
    (decrypt (:key-file-path conf) "AAAADH2MEPeapWrwrhmAOKGL+hglh++LIwGUoydlUrUm4H7RDtfw+ztNc8IEkMM3aFZawUBdHnJr45pW0qXAzhu9XddLXns/")))

(defn fetch-search-results! [search-term]
  "Makes request to freesound api to retreive the json for search
  results. This doesn't include mp3 previews, if you need mp3
  previews, use fetch-sound! below"
  (let [url (str "https://freesound.org/apiv2/search/text/?query=" search-term)]
    (client/get (str url "&token=" api-key) {:as :json})))

(defn fetch-sound! [sound-id]
  "Makes request to freesound api to retreive the json for a soundid"
  (if sound-id
    (let [url (str "https://freesound.org/apiv2/sounds/" sound-id "/" "?token=" api-key)
          response (client/get url {:as :json})]
      (:body response))))

(defn fetch-mp3! [mp3-url]
  "Make request for mp3 and return byte array stream"
  (let [res (client/get mp3-url {:as :byte-array})]
    (if (= (:status res) 200)
      (:body res))))

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

(defn play-file! [full-file-path]
  (let [fis (java.io.FileInputStream. full-file-path)]
    (play-input-stream! fis)))

;; TODO: move this "mp3" directory into config 
(defn search-and-play-file! [filename]
  (let [path-to-file (str "./mp3/" filename ".mp3")
        f (java.io.File. path-to-file)
        exists? (.exists f)]
    (if exists?
      (do
        (play-file! f)
        path-to-file)

      false)))

(defn get-preview-hq-mp3 [sound-result]
  "Get the url of the hq mp3 preview found in results from fetch-sound!"
  (let [sound-id (:id sound-result)
        mp3-url (:preview-hq-mp3 (:previews sound-result))]
    mp3-url))


;; Public API, these are the functions that callers should use
;; All of these implement caching so they won't make unnecessary
;; freesound api calls or unneccessarily download mp3 files

;; TODO move to config
(def mp3-cache-dir "./cache/mp3")

;; This is a map where keys are the guids that are generated from
;; searches for sounds from freesound api. The values are urls to the
;; freesound site. This is needed because mp3 files are cached, and we
;; don't always make requests to freesound. 
(def mp3-cache (atom {}))

(defn clear-mp3-cache []
  (reset! mp3-cache {})
  (let [all-files (file-seq (io/file mp3-cache-dir))
        mp3-files (filter (fn [f] (and (.isFile f)
                                       (= '(\m \p \3)
                                          (take-last 3 (.getName f)))))
                          all-files)]
    (doseq [f mp3-files]
      (io/delete-file f true))))

;; TODO refactor into a mp3.clj namespace maybe?
(defn fetch-mp3-from-url-and-play-and-cache! [mp3-url guid file-name]
  "Clear any existing file that exists for this sound-id, then
  Download the mp3 preview, play it, and save it for later."
  (io/delete-file file-name true)
  (let [stream (fetch-mp3! mp3-url)]
    (if (not (nil? stream))
      (let [bis (java.io.ByteArrayInputStream. stream)]
          (play-input-stream! bis)
          (swap! mp3-cache assoc guid mp3-url)
          (with-open [w (io/output-stream file-name)]
            (.write w stream))
          mp3-url))))

(defn play-mp3-from-url!
  ([mp3-url] (play-mp3-from-url! mp3-url false))
  ([mp3-url clear-cache?]
   (let [guid (md5 (str mp3-url))
         cached-url (get @mp3-cache guid nil)
         file-name (str mp3-cache-dir "/" guid ".mp3")
         f (io/file file-name)]

     (if (and (.exists f)
              (not clear-cache?))

       ;; if the file exists in cache, use it.
       (do
         (play-input-stream! (io/input-stream f))
         mp3-url)

       ;; else
       (fetch-mp3-from-url-and-play-and-cache!
        mp3-url guid file-name)))))

(defn fetch-mp3-and-play-and-cache! [sound-id guid file-name]
  "Clear any existing file that exists for this sound-id, then
  Download the mp3 preview, play it, and save it for later."
  (io/delete-file file-name true)
  (if-let [sound-result (fetch-sound! sound-id)]
    (let [url (:url sound-result)
          mp3-url (get-preview-hq-mp3 sound-result)
          stream (fetch-mp3! mp3-url)]
      (if (not (nil? stream))
        (let [bis (java.io.ByteArrayInputStream. stream)]
          (play-input-stream! bis)
          (swap! mp3-cache assoc guid url)
          (with-open [w (io/output-stream file-name)]
            (.write w stream))
          url)))))

(defn play-sound!
  ([sound-id] (play-sound! sound-id false))
  ([sound-id clear-cache?]

   (let [guid (md5 (str sound-id))
         url (get @mp3-cache guid nil)
         file-name (str mp3-cache-dir "/" guid ".mp3")
         f (io/file file-name)]

     (if (and (.exists f)
              (not clear-cache?)
              url)

       ;; if the file exists in cache, use it.
       (do
         (play-input-stream! (io/input-stream f))
         url)

       ;; else
       (fetch-mp3-and-play-and-cache! sound-id guid file-name)))))

(defn search-and-play-nth!
  ([search-term n] (search-and-play-nth! search-term n false))
  ([search-term n clear-cache?]

   (let [guid (md5 (str search-term n))
         url (get @mp3-cache guid nil)
         file-name (str mp3-cache-dir "/" guid ".mp3")
         f (io/file file-name)]

     (if (and (.exists f)
              (not clear-cache?)
              url)

       ;; if the file exists in cache, use it.
       (do
         (play-input-stream! (io/input-stream f))
         url)

       ;; otherwise, do a search for sounds
       (let [search-results (fetch-search-results! search-term) 
             sound-result (nth-search-result search-results n)
             sound-id (:id sound-result)]
         (fetch-mp3-and-play-and-cache! sound-id guid file-name))))))

(defn play-not-found! []
  (play-sound! 216090))
