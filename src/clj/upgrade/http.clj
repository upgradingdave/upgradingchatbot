(ns upgrade.web.http
  (:require [clojure.java.io :as io]
            [upgrade.common :refer [log]]
            [org.httpkit.server :refer [run-server]]
            [bidi.bidi :refer [match-route path-for]]
            [bidi.ring :refer (make-handler)]
            [ring.util.response :as res]))


(defn color-handler
  [request]
  (log (str request))
  (res/response (str request)))

(defn file-handler [path-to-file]
  (fn [request]
    (res/response (io/file (str "resources/public/" path-to-file)))))

(defn resources-handler
  [request]
  (let [uri (:uri request)
        path-to-file (str "resources/public" uri)
        file (io/file path-to-file)]
    (println path-to-file)
    (if (and file (.exists file))
      (res/response file)
      (res/response "file not found"))
    ))

(def routes
  ["/" {"color/query" color-handler
        "index.html" (file-handler "index.html")
        #"js/.+" resources-handler
        }])

(def handler (make-handler routes))

(defn app [req]
  (handler req))

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

