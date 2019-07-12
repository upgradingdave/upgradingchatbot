(ns ^:figwheel-hooks upgrade.followers
  (:require [ajax.core :as ajax]
            ["react-transition-group/Transition" :as Transition]
            ["react-transition-group/TransitionGroup" :as TransitionGroup]
            ["react-transition-group/CSSTransition" :as CSSTransition]    
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]
            [re-frame.router :refer [dispatch]]
            [re-frame.fx]
            [cognitect.transit :as transit]))

(def json-reader (transit/reader :json))

;; TODO move to common ns
(defn log [msg]
  (js/console.log msg))

;; state
(defonce ws-chan (atom nil))
(defonce timeouts (atom {}))

(rf/reg-event-db
 ::initialize
 (fn [_ _]
   (log (str "Initializing app-db"))
   {:running? false}))

(rf/reg-event-fx
 ::start
 (fn [cofx [_ msg]]
   (log (str "Start the animation"))
   (log msg)
   (let [{:keys [animation-key follower]} msg]
     {:db {animation-key {:follower follower
                          :running? true}}
      :dispatch-later [{:ms 20000 :dispatch [::stop]}]}
     )))

(rf/reg-event-db
 ::stop
 (fn [_ _]
   (log (str "Stop the animation"))
   {:followers {:running? false}}))

(rf/reg-sub
  ::running
  (fn [db _] (get-in db [:followers :running?] nil)))

(rf/reg-sub
  ::follower
  (fn [db _] (get-in db [:followers :follower] nil)))

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
      (= animation-key :followers)
      (rf/dispatch [::start msg])
      

      :else
      (log (str "[followers] Need to implement animation-key: "
                animation-key)))
    ))

(defn make-websocket! [url]
 (log "attempting to connect websocket")
 (if-let [chan (js/WebSocket. url)]
   (do
     (set! (.-onmessage chan) handle-ws-event)
     (reset! ws-chan chan))
   (throw (js/Error. "Websocket connection failed!"))))

;; TODO move this to a config file
(def ws-url "ws://localhost:8081/ws")

(defn view []
  (let [running? (rf/subscribe [::running])
        follower (rf/subscribe [::follower])]
    (fn []
      [:div {:class :follower}
       [:> CSSTransition {:in @running?
                          :classNames "follower__animation"
                          :timeout 5000
                          :on-enter #(log "enter")
                          :on-entering #(log "entering")
                          :on-entered #(log "entered")
                          :on-exit #(log "exit")
                          :on-exiting #(log "exiting")
                          :on-exited #(log "exited")}

        [:div {:class (if @running? "" "follower__animation--init")}
         [:p {:class :follower__msg} "Welcome to the crew, "]
         [:img {:src "/img/dancing_sailors.gif"}]
         [:p {:class "follower__name"} @follower "!"]]]

       ])))

(defn run []
  (log "[followers] run")
  ;; connect to websocket
  (make-websocket! ws-url)
  ;; setup initial state
  (rf/dispatch-sync [::initialize])
  (reagent/render [view]
                  (js/document.getElementById "app")))

(defonce start-up (do (run) true))

(defn ^:after-load restart []
  (log "[FIGWHEEL] restart")
  (run))
