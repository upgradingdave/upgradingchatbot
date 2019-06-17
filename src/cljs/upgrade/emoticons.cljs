(ns ^:figwheel-hooks upgrade.emoticons
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]
            [cognitect.transit :as transit]))

(def json-reader (transit/reader :json))

;; TODO move to common ns
(defn log [msg]
  (js/console.log msg))

(defn client-width []
  (.-clientWidth (.-documentElement js/document)))

(defn client-height []
  (.-clientHeight (.-documentElement js/document)))

(defn client-center []
  (js/createVector (/ (client-width) 2)
                   (/ (client-height) 2)))

(defn move [loc vel]
  "Add velocity to a location"
  (.add js/p5.Vector loc vel))

(defn guard-x [loc min max]
  "Ensure vector's x coord is never larger than max or less than min"
  (cond (< (.-x loc) min) (js/createVector min (.-y loc))
        (> (.-x loc) max) (js/createVector max (.-y loc))
        :else loc))

(defn guard-y [loc min max]
  "Ensure vector's y coord is never larger than max or less than min"
  (cond (< (.-y loc) min) (js/createVector (.-x loc) min)
        (> (.-y loc) max) (js/createVector (.-x loc) max)
        :else loc))

(defn guard [loc xmax ymax]
  "Convenience for setting up a guard for 0 < x < xmax and 0 < y < ymax"
  (-> (guard-x loc 0 xmax)
      (guard-y 0 ymax)))

(defn bounce-x [vel loc min max]
  "Reverse velocity's x coord when location is larger than max or less
  than min"
  (cond (<= (.-x loc) min) (js/createVector (- 0 (.-x vel)) (.-y vel))
        (>= (.-x loc) max) (js/createVector (- 0 (.-x vel)) (.-y vel))
        :else vel))

(defn bounce-y [vel loc min max]
  "Reverse velocity's y coord when location is larger than max or less
  than min"
  (cond (<= (.-y loc) min) (js/createVector (.-x vel) (- 0 (.-y vel)) )
        (>= (.-y loc) max) (js/createVector (.-x vel) (- 0 (.-y vel)) )
        :else vel))

(def canvas (atom nil))

;; state looks like this. one key for each animation
;; I call the keys `animation-key`
;; {:emoticons {:running? false
;;              :emote image
;;              :location vector
;;              :acceleration vector
;;              :velocity vector
;;              }
;; :followers {:running? false
;;              :emote image
;;              :location vector
;;              :acceleration vector
;;              :velocity vector
;;              }}

(def state (atom {:emoticons nil
                  :followers nil}))

;; (defn welcome-followers []
;;   (if-let [s (get-in @state [:followers])]

;;     ;; Paint the screen with the current state
;;     (let [{:keys [running?
;;                   location
;;                   velocity
;;                   acceleration
;;                   frames
;;                   follower
;;                   img]} s]

;;       (when (and running? (< frames 500))

;;         ;; display text
;;         (js/textSize 64)
;;         (js/fill (js/color 0 0 255))
;;         (js/text (str "Welcome to the crew, " follower "! ")
;;                  (.-x location) (.-y location))

;;         (when img
;;           (js/image img (.-x location) (.-y location))))

;;       ;; update the state. 
;;       (let [x-bound (client-width) 
;;             y-bound (client-height)

;;             location (-> location
;;                          (move velocity)                            
;;                          (guard x-bound y-bound))
            
;;             velocity (-> velocity
;;                          (bounce-x location 0 x-bound)
;;                          (bounce-y location 0 y-bound))
            
;;             frames (inc frames)

;;             animation-key :followers
;;             ]
        
;;         (swap! state assoc-in [animation-key :running?] running?)
;;         (swap! state assoc-in [animation-key :location] location)
;;         (swap! state assoc-in [animation-key :velocity] velocity)
;;         (swap! state assoc-in [animation-key :acceleration] acceleration)
;;         (swap! state assoc-in [animation-key :frames] frames))
;;       ))
;;   )

(defn draw-emoticons []
  (if-let [s (get-in @state [:emoticons])]
    ;; Paint the screen with the current state
    (let [{:keys [running?
                  emote
                  location
                  velocity
                  acceleration
                  frames]} s]

      (when (and running? (< frames 500))

        ;; display text
        ;; (js/textSize 16)
        ;; (js/fill 200)
        ;; (js/text (str "Location:        " (.toString location)) 10, 30)

        ;; display ball
        ;;(js/fill (js/color 0 0 255))
        ;;(js/ellipse (.-x location) (.-y location) obj-width)

        ;; dispaly image
        (when emote
            (js/image emote (.-x location) (.-y location)))

        ;; update the state. 
        (let [x-bound (- (client-width) (.-width emote))
              y-bound (- (client-height) (.-height emote))

              location (-> location
                           (move velocity)                            
                           (guard x-bound y-bound))
              velocity (-> velocity
                           (bounce-x location 0 x-bound)
                           (bounce-y location 0 y-bound))
              frames (inc frames)

              animation-key :emoticons
              ]

          (swap! state assoc-in [animation-key :running?] running?)
          (swap! state assoc-in [animation-key :emote?] emote)
          (swap! state assoc-in [animation-key :location] location)
          (swap! state assoc-in [animation-key :velocity] velocity)
          (swap! state assoc-in [animation-key :acceleration] acceleration)
          (swap! state assoc-in [animation-key :frames] frames)))
      )))

;;wink smile https://static-cdn.jtvnw.net/emoticons/v1/11/2.0
(defn preload []
  (log "[P5JS] preload"))

(defn setup []
  (log "[P5JS] setup")
  (js/frameRate 60)
  (let [c (js/createCanvas (client-width) (client-height))]
    (.parent c "my-canvas")
    (reset! canvas c)

    ;; ---- EMOTICONS -----
    (let [animation-key :emoticons]
      (swap! state assoc-in [animation-key :running?] false)
      (swap! state assoc-in [animation-key :frames] 0)
      ;; already created emote in preload
      ;; (swap! state assoc-in [animation-key :emote?] emote)
      (swap! state assoc-in [animation-key :location]
             (js/createVector (client-width)
                              (client-height)))
      (swap! state assoc-in [animation-key :velocity] (js/createVector 2.5 2))
      (swap! state assoc-in [animation-key :acceleration]
             (js/createVector 0.01 0.1)))

    ;; ---- FOLLOWERS -----
    (let [animation-key :followers]
      (swap! state assoc-in [animation-key :running?] false)
      (swap! state assoc-in [animation-key :frames] 0)
      ;; already created emote in preload
      ;; (swap! state assoc-in [animation-key :emote?] emote)
      (swap! state assoc-in [animation-key :location]
             (js/createVector (client-width)
                              (client-height)))
      (swap! state assoc-in [animation-key :velocity] (js/createVector 2.5 2))
      (swap! state assoc-in [animation-key :acceleration]
             (js/createVector 0.01 0.1)))
    
    ))

(defn draw []
  (js/clear)
  ;;(js/background 0)

  (draw-emoticons)
  ;;(welcome-followers)
  )

;; websockets
(defonce ws-chan (atom nil))

;; Example of emoticon img url
;; (js/loadImage "https://static-cdn.jtvnw.net/emoticons/v1/30259/2.5")
(defn handle-ws-event [evt]
  "This is an event handler for websocket messages. It's registered as
  WebSocket.onmessage"
  (log "Got a message!")
  (log evt)
  (let [msg (->> evt .-data (transit/read json-reader))
        animation-key (:animation-key msg)]

    (cond
      (= animation-key :emoticons)
      (let [url (:url msg)]
        (swap! state assoc-in [animation-key :emote] nil)
        (swap! state assoc-in [animation-key :running?] true)
        (swap! state assoc-in [animation-key :frames] 0)
        (swap! state assoc-in [animation-key :emote]
               (js/loadImage url)))

      (= animation-key :followers)
      (let [follower (:follower msg)]
        ;;(swap! state assoc-in [animation-key :emote] nil)
        (swap! state assoc-in [animation-key :running?] true)
        (swap! state assoc-in [animation-key :frames] 0)
        (swap! state assoc-in [animation-key :follower] follower)
        (swap! state assoc-in [animation-key :img]
               (js/loadImage "/img/dancing_sailors.gif")))

      :else
      (log (str "Need to implement animation-key: " animation-key)))

    ))

(defn make-websocket! [url]
 (log "attempting to connect websocket")
 (if-let [chan (js/WebSocket. url)]
   (do
     (set! (.-onmessage chan) handle-ws-event)
     (reset! ws-chan chan))
   (throw (js/Error. "Websocket connection failed!"))))

;; TODO move this to a config file
(def url "http://localhost:8081")
(def ws-url "ws://localhost:8081/ws")

(defn view []
  (let []
    (fn []
      [:div {:style {:position "absolute" :margin "auto"}}
       [:img {:src "/img/dancing_sailors.gif"}]])))

(defn run []
  (log "[P5JS] run")
  ;; connect to websocket
  (make-websocket! ws-url)
  ;; create sketch
  (doto js/window
    (aset "setup" setup)
    (aset "draw" draw)
    (aset "preload" preload))

  ;; (reagent/render [view]
  ;;                 (js/document.getElementById "app"))
  )

(defonce start-up (do (run) true))

(defn ^:after-load restart []
  (log "[FIGWHEEL] restart")
  (preload)
  (setup)
  (run))


