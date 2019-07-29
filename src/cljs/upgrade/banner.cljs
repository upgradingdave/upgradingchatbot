(ns ^:figwheel-hooks upgrade.banner
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
    :msg "Ahoy There!"}))

(rf/reg-event-db
 ::new-message
 (fn [db [_ payload]]
   (log (str "new-message event: " payload))
   (assoc-in db [:msg] (:msg payload))))

(rf/reg-sub
  ::msg
  (fn [db _] (get-in db [:msg] nil)))

(defn handle-ws-event [evt]
  "This is an event handler for websocket messages. It's registered as
  WebSocket.onmessage"
  (log "Got a message!")
  (log evt)
  (let [msg (->> evt .-data (transit/read json-reader))
        message-type (:animation-key msg)]

    (cond
      (= message-type :banner-message)
      (let []
        (rf/dispatch [::new-message (:payload msg)]))

      :else
      (log (str "[chat] Need to implement message-type: "
                message-type)))))

(defn view []
  (let [msg (rf/subscribe [::msg])]
    (fn []
      [:div {:class :page}
       [:div {:class :spacer}]
       [:div {:class :banner}

        [:div {:class :banner__top}]
        [:div {:class :banner__main}
         [:p @msg]]]

       [:div {:class :camera}]])))

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


