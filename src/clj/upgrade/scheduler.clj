(ns upgrade.scheduler
  (:require [java-time :as t]
            [overtone.at-at :as at]
            [upgrade.common :as c]
            [upgrade.twitchbot :refer [send-message]]))

(def pool (atom (at/mk-pool)))

(defn mins-to-millis
  "convert minutes to milliseconds"
  [mins]
  (* (* 60 mins) 1000))

(defn schedule-repeating-message
  [client channel message interval-millis initial-delay-millis]
  (at/every interval-millis
            #(send-message client channel message)
            @pool
            :initial-delay initial-delay-millis))

(defn resolve-scheduled-message
  "Find the message that corresponds with scheduled-message and update
  scheduled-message with actual message content and key"
  [scheduled-message messages]
  (let [{:keys [message key]} (get messages (:message-id scheduled-message))]
    (assoc scheduled-message :message message :key key)))

(defn resolve-scheduled-messages [scheduled-messages messages]
  (map #(resolve-scheduled-message % messages) scheduled-messages))

;; TODO refactor to use spec to validate datastructure from config file
;; Here's what I expect the `scheduled-messages` data structure to look like: 
;; [{:message-id 0
;;  :repeat-millis (* 15 60 1000)
;;   :delay-millis 0
;;   :message ""
;;   :key ""}
;; ...
;; ]
(defn schedule-repeating-messages [client channel scheduled-messages]
  (map (fn [{:keys [message repeat-millis delay-millis]}]
         (schedule-repeating-message client channel message
                                     repeat-millis delay-millis))
       scheduled-messages))

(comment

  ;; get scheduled-messages from config.edn
  (def config (c/read-config-from-file "config.edn"))
  (def scheduled-messages (:scheduled-messages config))
  (def messages (:messages config))
  (def smsgs (resolve-scheduled-messages (:scheduled-messages config)
                                         (:messages config)))

  ;; schedule the jobs
  (def client (:client (:twitchbot sysconfig)))
  (def channel (:channel (:twitchbot sysconfig)))  
  (def results (schedule-repeating-messages client channel smsgs))

  ;; after jobs are scheduled, we can manage them like this
  (at/show-schedule @pool)
  (at/scheduled-jobs @pool)
  (at/stop-and-reset-pool! @pool)
  (at/stop-and-reset-pool! @pool :strategy :kill)
  (at/stop (first results))
  (at/kill (first results))
  
  

  ;; Other experiments
  ;; I think `t/local-date-time` doesn't include timezones? So, I'm going to
  ;; prefer `t/instant` .. but need to learn more
  ;; 10 seconds from now
  (t/plus (t/instant) (t/seconds 10))
  ;; 5 mins from now
  (t/plus (t/instant) (t/minutes 5))


  (at/at (t/plus (t/instant) (t/minutes 2))
         #(send-message client channel message)
         @pool)

  ;; schedule the "today" message to display every 20 minutes,
  ;; starting 2 minutes from now
  (let [twenty-mins (mins-to-millis 20)
        two-mins (mins-to-millis 2)]
    (every twenty-mins
           #(send-message client channel (today-message))
           @pool
           :initial-delay two-mins))

;; Just an experiment to send a message 10 seconds from now
;; (defn send-chat-message [client channel message]
;;   (at/at (t/plus (t/instant) (t/seconds 10))
;;          #(send-message client channel message)
;;          @pool))


;; today should show up every 15 minutes (from start time)
;; welcome should show up every 30 minutes
;; github should show up every 45 minutes

  )

