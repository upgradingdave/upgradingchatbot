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
    :chat-msgs [{:nick "upgradingchatbot"
                 :msg "Webchat is ALIVE!"}]}))

(rf/reg-event-db
 ::new-chat-message
 (fn [db [_ payload]]
   (let [chat-msgs (conj (:chat-msgs db) payload)]
     (log (str "new-chat-message event: " payload))
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
        (rf/dispatch [::new-chat-message (:payload msg)]))

      :else
      (log (str "[chat] Need to implement animation-key: "
                animation-key)))))

(defn view []
  (let [chat-msgs (rf/subscribe [::chat-msgs])]
    (fn []
      [:div {:class :page}
       [:div {:class :chat}
        [:div {:class :chat__gutter}]
        [:div {:class :chat__message-list}
         (map
          (fn [payload]
            (let [msg (:msg payload)
                  nick (:nick payload)]
              ^{:key (gensym "key-")}
              [:div {:class "chat__msg"}
               [:div {:class "chat__nick"} (str nick ": ")]
               [:div {:class "chat_body"} msg]]))
          @chat-msgs
          )]
        ]
       [:div {:class :footer}
        [:div {:class :footer__top}]
        [:div {:class :footer__main}

         [:p "Hi there! "]
         
         ]]
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


