(ns cam-server.routes.home
  (:require [cam-server.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! put! close! go]]
            ))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

;; chord.http-kit/with-channel is a wrapper around http-kit’s with-channel
;; macro which uses core.async’s primitives to interface with the channel.

(defn ws-handler [req]
  (with-channel req ws-ch
    (go
      (let [{:keys [message]} (<! ws-ch)]
        (prn "Message received:" message)
        (>! ws-ch "Hello client from server!")
        (close! ws-ch)))))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))

  (GET "/ws" req (ws-handler req))
  (POST "/ws" req (ws-handler req))
  )
