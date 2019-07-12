(ns upgrade.util
  (:require [cognitect.transit :as transit]))

(defn log [msg]
  (js/console.log msg))

(defonce ws-chan (atom nil))
(def json-reader (transit/reader :json))
(def ws-url "ws://localhost:8081/ws")

(defn make-websocket! [handle-ws-event]
 (log "attempting to connect websocket")
 (if-let [chan (js/WebSocket. ws-url)]
   (do
     (set! (.-onmessage chan) handle-ws-event)
     (reset! ws-chan chan))
   (throw (js/Error. "Websocket connection failed!"))))
