(ns upgrade.web.http
  (:require [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [files not-found]]
            [compojure.core :refer[defroutes GET POST DELETE ANY context]]))

(defn show-landing-page []
  (html [:div "hello world!"]))

(defroutes all-routes
  (GET "/" [] show-landing-page)
  (files "/static/") ;; static file url prefix /static, in `public` folder
  (not-found "<p>Page not found.</p>"))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (run-server #'app {:port 8080})))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (start-server))


;;(run-server app {:port 8080})


