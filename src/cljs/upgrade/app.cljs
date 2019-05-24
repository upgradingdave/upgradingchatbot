(ns upgrade.app
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]))

;; https://static-cdn.jtvnw.net/emoticons/v1/30259/1.0

;; Manage Twitch Extensions here:
;;https://dev.twitch.tv/console/extensions
;; Docs are here: 
;;https://dev.twitch.tv/docs/extensions

;; TODO move this to a config file
(def url "http://localhost:8081")

(def twitch js/window.Twitch.ext)

(defn log [msg]
  (js/console.log msg)
  (twitch.rig.log msg))

;; EVENTS

(rf/reg-event-db
 :initialize
 (fn [_ [_ token]]
   (log (str "Initializing app-db"))
   {:token token}))

;; TODO: implement Loading
(rf/reg-event-db
 :color/result
 (fn [db [_ result]]
   (assoc db :color/query-response result)))

(rf/reg-event-db
 :ajax/fail
 (fn [db [_ result]]
   (log "hmmm, failing??!!")
   (assoc db :ajax/fail-response result)))

(rf/reg-event-fx
 :color/query
 (fn [{:keys [db]} _]
   (let [token (:token db)]
     {:db (assoc db :loading true)
      :http-xhrio {:method :get
                   :headers {"Authorization" (str "Bearer " token)}
                   :uri (str url "/color/query")
                   :timeout 8000
                   ;;:response-format (ajax/json-response-format {:keywords? true})
                   :response-format (ajax/raw-response-format)
                   :on-success [:color/result]
                   :on-failure [:ajax/fail]}})))

(rf/reg-event-fx
 :color/cycle
 (fn [{:keys [db]} _]
   (let [token (:token db)]
     {:db (assoc db :loading true)
      :http-xhrio {:method :post
                   :headers {"Authorization" (str "Bearer " token)}                 
                   :uri (str url "/color/cycle")
                   :params nil
                   :timeout 5000
                   :format (ajax/json-request-format)
                   ;;:response-format (ajax/json-response-format {:keywords? true})
                   :response-format (ajax/raw-response-format)                 
                   :on-success [:color/result]
                   :on-failure [:ajax/fail]}})))

;; SUBSCRIPTIONS

(rf/reg-sub
  :color/query-response
  (fn [db _] (:color/query-response db)))

(rf/reg-sub
  :ajax/fail-response
  (fn [db _] (:ajax/fail-response db)))

(rf/reg-sub
  :twitch/token
  (fn [db _] (:token db)))

;; VIEWS

(defn view []
  (let [color-query-response (rf/subscribe [:color/query-response])
        ajax-fail (rf/subscribe [:ajax/fail-response])
        token (rf/subscribe [:twitch/token])]
    (fn []
      [:div

       (if @token
         [:div {:style {:color :green}} "Authorized!"]
         [:div {:style {:color :red}} "Not Yet Authorized"])
       
       ;; Ajax Troubleshooting
       [:div
        [:button {:on-click (fn [evt] (rf/dispatch [:color/query])) }
         "GET color/query"]
        [:button {:on-click (fn [evt] (rf/dispatch [:color/cycle])) }
         "POST color/cycle"]
        [:div {:style {:color "green"}}
         [:pre [:code (with-out-str (cljs.pprint/pprint @color-query-response))]]]
        [:div {:style {:color "red"}}
         [:pre [:code (with-out-str (cljs.pprint/pprint @ajax-fail))]]]]

        ;; color circle
       [:div {:style {:float "left" :position "relative" :left "10%"}}
        [:div {:id "color"
               :style {:border-radius "50px"
                       :transition "background-color 0.5s ease"
                       :margin-top "30px"
                       :width "100px"
                       :height "100px"
                       :background-color (or @color-query-response "#6441A4")
                       :float "left"
                       :position "relative"
                       :left "-50%"
                       }}]

        ]])))

;; Twitch Specific Stuff
(.onAuthorized
 twitch
 (fn [auth]
   (let [channelId (.-channelId auth)
         clientId  (.-clientId auth)
         token (.-token auth)
         userId (.-userId auth)] 
     (log "Successfully Authorized by Twitch")

     (rf/dispatch-sync [:initialize token])
     )))

(defn run
  []
  (reagent/render [view]
                  (js/document.getElementById "app")))

(run)

