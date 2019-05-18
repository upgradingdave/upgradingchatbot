(ns upgrade.app
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [reagent.core :as reagent]
            [re-frame.db :as db]
            [re-frame.core :as rf]))

(def twitch js/window.Twitch.ext)


;; EVENTS

(rf/reg-event-db
 :initialize
 (fn [_ _] {}))

;; TODO: implement Loading
(rf/reg-event-db
 :color/result
 (fn [db [_ result]]
   (assoc db :color/query-response result)))

(rf/reg-event-db
 :ajax/fail
 (fn [db [_ result]]
   (assoc db :ajax/fail-response result)))

(rf/reg-event-fx
 :color/query
 (fn [{:keys [db]} _]
   {:db (assoc db :loading true)
    :http-xhrio {:method :get
                 :uri "http://localhost:8081/color/query"
                 :timeout 8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:color/result]
                 :on-failure [:ajax/fail]}}))

(rf/reg-event-fx
 :color/cycle
 (fn [{:keys [db]} _]
   {:db (assoc db :loading true)
    :http-xhrio {:method :post
                 :uri "http://localhost:8081/color/cycle"
                 :params nil
                 :timeout 5000
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:color/result]
                 :on-failure [:ajax/fail]}}))

;; SUBSCRIPTIONS

(rf/reg-sub
  :color/query-response
  (fn [db _] (:color/query-response db)))

(rf/reg-sub
  :ajax/fail-response
  (fn [db _] (:ajax/fail-response db)))


;; VIEWS

(defn view []
  (let [color-query-response (rf/subscribe [:color/query-response])
        ajax-fail (rf/subscribe [:ajax/fail-response])]
    (fn []
      [:div

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

       ;; color cycle controls
       [:div
        [:div "Would you care to cycle a color?"]
        [:button {:on-click (fn [evt] (rf/dispatch [:color/cycle]))
                  :disabled (nil? @color-query-response)}
         "Yes, I would"]
        ]
       
        ;; color circle
       [:div {:style {:float "left" :position "relative" :left "10%"}}
        [:div {:id "color"
               :style {:border-radius "50px"
                       :transition "background-color 0.5s ease"
                       :margin-top "30px"
                       :width "100px"
                       :height "100px"
                       :background-color (or (:color @color-query-response) "#6441A4")
                       :float "left"
                       :position "relative"
                       :left "-50%"
                       }}]

        ]])))

(defn run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [view]
                  (js/document.getElementById "app")))

(run)

