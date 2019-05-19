(ns upgrade.system
  (:require [upgrade.common :refer [log get-config]]
            [upgrade.twitchbot :as bot]
            [upgrade.http :as http]))

;; This namespace contains all the nasty state management code.

(defonce system (atom {}))

(defn start-component-really! [component-kw component start-fn]
  (let [component (assoc
                   (start-fn component)
                   :running? true)]
    (swap! system assoc component-kw component)))

(defn start-component!
  "Generic, idempotent function to start a component. If the component
  was already started, this just returns the system map. If component
  has never been started then we try to get config and create the
  component for the first time. If the component is not running, we
  try to restart it"
  [component-kw start-fn]
  (let [component (get @system component-kw)]
    (if (nil? component)
      ;; component has never been started, so start it using config
      (start-component-really! component-kw (get (get-config) component-kw) start-fn)

      ;; component has been started before, check if it's running
      (if (:running? component)
        ;; if running, just return it
        @system

        ;; otherwise, restart component
        (start-component-really! component-kw component start-fn)))))

(defn stop-component!
  "Generic, idempotent function to stop a component"
  [component-kw stop-fn]
  (let [component (get @system component-kw)]
    (if (:running? component)
      (let [component (assoc
                       (stop-fn component)
                       :running? false)]
        (swap! system assoc component-kw component))
      ;; if not running, just return it
      @system)))

(defn start-twitchbot! []
  (start-component! :twitchbot bot/start-twitchbot!))

(defn stop-twitchbot! []
  (stop-component! :twitchbot bot/stop-twitchbot!))

(defn start-httpkit! []
  (start-component! :httpkit http/start-httpkit!))

(defn stop-httpkit! []
  (stop-component! :httpkit http/stop-httpkit!))

(defn start-system!
  ([config]
   (start-twitchbot! config)
   (start-httpkit! config)))

(defn stop-system!
  ([] (start-system! (get-config)))
  ([config]
   (start-twitchbot! config)
   (start-httpkit! config)))

;; TODO implement command line arg parsing
(defn -main [& args])


