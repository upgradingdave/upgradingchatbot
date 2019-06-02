(ns upgrade.system
  (:require [upgrade.common :refer [log get-config]]
            [upgrade.twitchbot :as bot]
            [upgrade.http :as http]
            [upgrade.twitch :as twitch]))

(defonce system (atom {}))

(defn start-component!
  "Generic, idempotent function to stop a component"
  [kw start-fn]
  (swap!
   system
   (fn [current]
     (let [component (get current kw)
           conf (get-config)]
       (if (or (nil? component) (not (:running? component)))
         (assoc current kw
                ;; this is subtle, but be careful to pass the current system state
                ;; to component's start method
                (-> (start-fn (assoc current kw (get conf kw)))
                    (assoc :running? true)))
         
         ;; else
         current)))))

(defn stop-component!
  "Generic, idempotent function to stop a component"
  [kw stop-fn]
  (swap!
   system
   (fn [current]
     (let [component (get current kw)]
       (if (:running? component)
         (assoc current kw
                (-> (stop-fn current)
                    (assoc :running? false)))
         
         ;; else
         current)))))

(defn start-twitchbot!
  "Start a twitchbot and update httpkit's dependency on twitchbot"
  []
  (let [{:keys [twitchbot]} (start-component! :twitchbot bot/start-twitchbot!)]
    (swap! system assoc-in [:httpkit :twitchbot] twitchbot)))

(defn stop-twitchbot! []
  (stop-component! :twitchbot bot/stop-twitchbot!))

(defn start-httpkit! []
  (start-component! :httpkit http/start-httpkit!))

(defn stop-httpkit! []
  (stop-component! :httpkit http/stop-httpkit!))

(defn start-system! []
  (start-twitchbot!)
  (start-httpkit!))

(defn stop-system! []
  (stop-twitchbot!)
  (stop-httpkit!))

;; TODO implement command line arg parsing
(defn -main [& args])



