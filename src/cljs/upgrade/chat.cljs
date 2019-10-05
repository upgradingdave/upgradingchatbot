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
    :last-scroll-position 0
    :scrolled-up false
    :chat-msgs

    ;; default initial message
    [{:nick "upgradingchatbot"
      :msg "Webchat is ALIVE!"}]

    ;; uncomment to initialize chat with a bunch of messages (nice for
    ;; testing auto scrolling)
    
    ;; (into [] 
    ;;       (for [x (range 20)]
    ;;         {:nick "upgradingchatbot"
    ;;          :msg (str x " Webchat is ALIVE!")}))
    }))

(rf/reg-event-db
 ::scroll-change
 (fn [db [_ scroll-position]]
   (log (str "Handle scroll change"))
   (let [last-scroll-position (:last-scroll-position db)
         scrolled-up (:scrolled-up db)
         scroll-diff (- last-scroll-position scroll-position)
         is-scrolling-up (> scroll-diff 0)]
     (log is-scrolling-up)
     (-> db
         (assoc-in [:last-scroll-position] scroll-position)
         (assoc-in [:scrolled-up] is-scrolling-up)))))

(rf/reg-event-db
 ::new-chat-message
 (fn [db [_ payload]]
   (let [chat-msgs (conj (:chat-msgs db) payload)]
     ;;(log (str "new-chat-message event: " payload))
     (assoc-in db [:chat-msgs] chat-msgs))))

(rf/reg-sub
  ::chat-msgs
  (fn [db _] (get-in db [:chat-msgs] nil)))


;; TODO think about how to make this generic and how to have multiple
;; components hook into this
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
        (rf/dispatch [::new-chat-message (:payload msg)]))

      :else
      (log (str "[chat] Need to implement animation-key: "
                animation-key)))))


(defn auto-scroll?
  "If a user just scrolled up, then we should pause automatic
  scrolling. If user or something else just scrolled down, then we
  need to figure out how far we are from the bottom. The
  min-height-from-bottom adds some wiggle room. For example, if
  min-height-from-bottom is 100, then auto scrolling will remain
  enabled until the scrollbar is at least 100 above bottom."
  [el last-scroll-position min-height-from-bottom]
  (let [scroll-position (.-scrollTop el)
        scroll-diff (- last-scroll-position scroll-position)
        is-scrolling-up (> scroll-diff 0)
        scroll-height (.-scrollHeight el)
        height-diff (- scroll-height scroll-position)
        client-height (.-clientHeight el)
        height-from-bottom (- height-diff client-height)]

    ;; (log (str "----- LET'S SCROLL -----"))
    ;; (log (str "last-scroll-position: " last-scroll-position))
    ;; (log (str "is-scrolling-up: " is-scrolling-up))
    ;; (log (str "scroll-diff: " scroll-diff))
    ;; (log (str "scroll-height: " scroll-height))    
    ;; (log (str "client height: " client-height))
    ;; (log (str "height diff: " height-diff))
    ;; (log (str "height-from-bottom: " height-from-bottom))
        
    (< height-from-bottom min-height-from-bottom)))

(defn scroll-to-bottom
  "If element el is scrollable, this will force the scrollbar to the
  bottom"
  [el]
  (.scrollTo el 0 (.-scrollHeight el)))

(defn children-height
  "Calculate total height of an elements children"
  [el]
  (let [children (array-seq (.-children el))
        total (reduce + (map #(.-clientHeight %) children))]
    total))

(defn overflowing?
  "Determine if an element's children are overflowing outside of the element"
  [el]
  (let [cheight (children-height el)
        myheight (.-clientHeight el)]
    (> cheight myheight)))

(defn sea-creature-img [image-file-name custom-style]
  [:img {:class :chat-wrapper__octo
         :src (str "/img/" image-file-name)
         :style (merge {:width "200px"
                        :opacity "0.2"}
                       custom-style)}])

(defn chat-view
  ""
  [] ;; remember to repeat any params here in render below
  (let [chat-msgs (rf/subscribe [::chat-msgs])
        chat-el (atom nil)
        last-scroll-position (atom 0)
        is-auto-scroll-enabled (atom true)
        overflowing (atom false)]
    (reagent/create-class
     {:display-name  "upgradingchatbot-chat-view"

      :component-did-mount
      (fn [this] 
        (log "component-did-mount")
        (scroll-to-bottom @chat-el))
         
      :component-did-update
      (fn [this old-argv]
        (log "component-did-update")
        (reset! overflowing (overflowing? @chat-el))
        (when @is-auto-scroll-enabled
          (scroll-to-bottom @chat-el)))
      
      :reagent-render
      (fn [] ;; remember to repeat params from above (if any)
        [:div {:class :chat-wrapper}
         [:div {:class :chat}
          [:div {:class :chat__gutter}]
          [:div {:class :chat__message-list
                 :style (when (not @overflowing) {:justify-content :flex-end})
                 :ref (fn [el] (reset! chat-el el))
                 :on-scroll
                 (fn [e]
                   (let [should-auto-scroll (auto-scroll? @chat-el
                                                          @last-scroll-position
                                                          40)
                         new-scroll-position (.-scrollTop @chat-el)]

                     (reset! last-scroll-position new-scroll-position)
                     (reset! is-auto-scroll-enabled should-auto-scroll)

                     ;; (log (str "should we scroll? " should-auto-scroll))

                     ))}
           (map
            (fn [payload]
              (let [msg (:msg payload)
                    nick (:nick payload)]
                ^{:key (gensym "key-")}
                [:div {:class "chat__msg"}
                 [:div {:class "chat__msg__nick"} (str nick ": ")]
                 [:div {:class "chat__msg__body"} msg]]))
            @chat-msgs
            )]]

         ;; I didn't really like how this turned out and it screwed up
         ;; the scrolling

         ;; [sea-creature-img "octopus4.png" {:top :160px
         ;;                                   :transform "rotate(330deg)"}]
         ;; [sea-creature-img "polypus.png" {:top :300px
         ;;                                  :opacity "0.5"
         ;;                                  :transform "rotate(-40deg)"}]
         ;; [sea-creature-img "octopus6.png" {:top :545px
         ;;                                   :opacity "0.3"
         ;;                                   :transform "rotate(120deg)"}]
         ])
      })))

(defn view []
  (let [chat-msgs (rf/subscribe [::chat-msgs])]
    (fn []
      [:div {:class :page}
       [chat-view]])))

(defn run []
  (log "[chat] run")

  ;; connect to websocket
  (make-websocket! handle-ws-event)

  ;; setup initial state
  (rf/dispatch-sync [::initialize])
  (reagent/render [view]
                  (js/document.getElementById "app")))

(defn ^:export start-up []
  (do (run) true))

(defn ^:after-load restart []
  (log "[FIGWHEEL] restart")
  ;;(run)
  )
