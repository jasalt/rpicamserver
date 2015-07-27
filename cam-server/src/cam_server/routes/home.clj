(ns cam-server.routes.home
  (:require [cam-server.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [chord.http-kit :refer [wrap-websocket-handler with-channel]]
            [clj-uuid :as uuid]
            [clojure.core.async :refer [<! >! put! close! timeout
                                        go go-loop] :as a]
            [cam-server.camera :as camera]
            ))

(defn home-page []
  (layout/render
   "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

(defn send-loop [client-channel]
  (go-loop [acc 0]
    (<! (timeout 2000))
    (let [image (camera/take-b64-pic!)]
      (>! client-channel image)  
      )
    (recur (inc acc))
    )
  )

;; chord.http-kit/with-channel is a wrapper around http-kit’s with-channel
;; macro which uses core.async’s primitives to interface with the channel.

(defn ws-handler [{:keys [ws-channel] :as req}]
  (println "Opened connection from" (:remote-addr req))
  (go-loop []
    (when-let [{:keys [message error] :as msg} (<! ws-channel)]
      (prn "Message received:" msg)
      ;; (>! ws-channel (if error
      ;;                  (format "Error: '%s'." (pr-str msg))
      ;;                  {:received (format "You passed: '%s' at %s."
      ;;                                     (pr-str message) (java.util.Date.))}))
      (recur)))
  (send-loop ws-channel)
  )

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))

  (GET "/ws" req (-> ws-handler
                     (wrap-websocket-handler {:format :transit-json})))
  )
