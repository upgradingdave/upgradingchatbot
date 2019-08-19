(ns ^:figwheel-hooks upgrade.overlay
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
            [upgrade.chat :refer [chat-view]]
            [upgrade.util :refer [log
                                  make-websocket!
                                  json-reader]]))

(rf/reg-event-db
 ::initialize
 (fn [_ _]
   (log (str "Initializing app-db"))
   {:init true
    :chat-msgs [{:nick "upgradingchatbot"
                 :msg "Webchat is ALIVE!"}]
    }))

(defn handle-ws-event
  "This is an event handler for websocket messages. It's registered as
  WebSocket.onmessage"
  [evt]
  ;;(log "Got a message!")
  ;;(log evt)
  (let [msg (->> evt .-data (transit/read json-reader))
        animation-key (:animation-key msg)]

    (cond
      (= animation-key :chat)
      (let []
        (rf/dispatch [:upgrade.chat/new-chat-message (:payload msg)]))

      :else
      (log (str "[chat] Need to implement animation-key: "
                animation-key)))))

(defn overlay-view
  "Main overlay component that is displayed"
  [] ;; remember to repeat any params here in render below
  (let [] ;; local state if needed
    (fn [] ;; remember to repeat params from above (if any)
      [:div {:class :overlay}
       [chat-view]])))

(defn view []
  (let []
    (fn []
      [:div {:class :page}
       [overlay-view]])))

(defn run []
  (log "[overlay] run")

  ;; connect to websocket
  (make-websocket! handle-ws-event)

  ;; setup initial state
  (rf/dispatch-sync [::initialize])
  (reagent/render [view]
                  (js/document.getElementById "app")))

;; TODO: how to switch between testing different namespaces without
;; having to comment / uncomment this line??
(defonce start-up (do (run) true))

(defn ^:after-load restart []
  (log "[FIGWHEEL] restart")
  ;;(run)
  )


