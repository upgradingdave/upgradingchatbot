(ns ^:figwheel-hooks upgrade.emoticons
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]))

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

(def app-key :emoticons)
(def canvas (atom nil))

;; state looks like this
;; {:emoticons {:running? false
;;              :emote image
;;              :location vector
;;              :acceleration vector
;;              :velocity vector
;;              }}
(def state (atom {app-key nil}))

(defn draw-emoticons []
  (if-let [s (get-in @state [app-key])]
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
              ]

          (swap! state assoc-in [app-key :running?] running?)
          (swap! state assoc-in [app-key :emote?] emote)
          (swap! state assoc-in [app-key :location] location)
          (swap! state assoc-in [app-key :velocity] velocity)
          (swap! state assoc-in [app-key :acceleration] acceleration)
          (swap! state assoc-in [app-key :frames] frames)))
      )))

(defn preload []
  (log "[P5JS] preload")

  ;; preload any images we might need
  (swap! state assoc-in [app-key :emote]
         (js/loadImage "https://static-cdn.jtvnw.net/emoticons/v1/30259/2.5")))

(defn setup []
  (log "[P5JS] setup")
  (js/frameRate 60)
  (let [c (js/createCanvas (client-width) (client-height))]
    (.parent c "my-canvas")
    (reset! canvas c)

    ;; setup initial state for emoticons
    (swap! state assoc-in [app-key :running?] false)
    (swap! state assoc-in [app-key :frames] 0)
    ;; already created emote in preload
    ;; (swap! state assoc-in [app-key :emote?] emote)
    (swap! state assoc-in [app-key :location] (js/createVector (client-width)
                                                               (client-height)))
    (swap! state assoc-in [app-key :velocity] (js/createVector 2.5 2))
    (swap! state assoc-in [app-key :acceleration] (js/createVector 0.01 0.1))    
))

(defn draw []
  (js/clear)
  ;;(js/background 0)

  (draw-emoticons))

;; websockets
(defonce ws-chan (atom nil))

(defn handle-ws-event [evt]
  "This is an event handler for websocket messages. It's registered as
  WebSocket.onmessage"
  (log "Got a message!")
  (log evt)
  (let [msg (js->clj (.-data evt))]
    (cond (= msg "HeyGuys")
          (do
            (swap! state assoc-in [app-key :running?] true)
            (swap! state assoc-in [app-key :frames] 0))


          :else
          (log (str "Websocket handler for: " msg " NOT yet implemented")))))

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

(defn run []
  (log "[P5JS] run")
  ;; connect to websocket
  (make-websocket! ws-url)
  ;; create sketch
  (doto js/window
    (aset "setup" setup)
    (aset "draw" draw)
    (aset "preload" preload)))

(defonce start-up (do (run) true))

(defn ^:after-load restart []
  (log "[FIGWHEEL] restart")
  (preload)
  (setup)
  (run)
  )


