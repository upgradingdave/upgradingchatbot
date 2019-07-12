(ns ^:figwheel-hooks upgrade.chat
  (:require [ajax.core :as ajax]
            ["react-transition-group/Transition" :as Transition]
            ["react-transition-group/TransitionGroup" :as
            TransitionGroup]
            ["react-transition-group/CSSTransition" :as CSSTransition]
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]
            [re-frame.router :refer [dispatch]]
            [re-frame.fx]
            [cognitect.transit :as transit]
            [upgrade.util :refer [log
                                  make-websocket!
                                  json-reader]]))

(rf/reg-event-db
 ::initialize
 (fn [_ _]
   (log (str "Initializing app-db"))
   {:init true
    :chat-msgs ["Just some test chats" "Here's another one"]}))

(rf/reg-event-db
 ::new-chat-message
 (fn [db [_ msg]]
   (let [chat-msgs (conj (:chat-msgs db) msg)]
     (log (str "new-chat-message event: " msg))
     (assoc-in db [:chat-msgs] chat-msgs))))

(rf/reg-sub
  ::chat-msgs
  (fn [db _] (get-in db [:chat-msgs] nil)))

(defn handle-ws-event [evt]
  "This is an event handler for websocket messages. It's registered as
  WebSocket.onmessage"
  (log "Got a message!")
  (log evt)
  (let [msg (->> evt .-data (transit/read json-reader))
        animation-key (:animation-key msg)]

    (cond
      (= animation-key :chat)
      (let []
        (rf/dispatch [::new-chat-message (:msg msg)]))

      :else
      (log (str "[chat] Need to implement animation-key: "
                animation-key)))))

(defn view []
  (let [chat-msgs (rf/subscribe [::chat-msgs])]
    (fn []
      [:div {:class :page}
       [:div {:class :chat}
        (map
         (fn [msg]
           ^{:key (gensym "key-")}[:div {:class "chat__msg"} msg])
         @chat-msgs
         )]
       [:div {:class :footer} "This is my footer"]
       ])))

(defn run []
  (log "[chat] run")
  ;; connect to websocket
  (make-websocket! handle-ws-event)
  ;; setup initial state
  (rf/dispatch-sync [::initialize])
  (reagent/render [view]
                  (js/document.getElementById "app")))

(defonce start-up (do (run) true))

(defn ^:after-load restart []
  ;;(log "[FIGWHEEL] restart")
  (run))


