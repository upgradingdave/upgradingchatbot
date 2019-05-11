(ns upgrade.web.http
  (:require [upgrade.common :refer [log]]
            [org.httpkit.server :refer [run-server]]
            [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [ring.util.response :as res]))


(defn color-handler
  [request]
  (println (str request))
  (log (str request))
  (res/response (str request)))

(def routes ["/color/query" color-handler])

(def handler (make-handler routes))

(defn app [req]
  (let [route (handler req)]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (str route)}
    ))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (run-server #'app {:port 8081})))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (start-server))

;;(run-server app {:port 8080})


