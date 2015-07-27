(ns ^:figwheel-always cam-client.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [cam-client.input]
     [cam-client.config]
     [cam-client.hud]
     
     [cljs.core.async :as async :refer [<! >! chan]]
     [chord.client :refer [ws-ch]]
     [reagent.core :as dom]
     ;;[cam-client.game :refer [game-canvas ball-entity pad-entity]]
     ))

(enable-console-print!)

;; # https://github.com/jarohen/chord
;; chord.client/ws-ch takes a web-socket URL and returns a map, containing
;; either :ws-channel or :error. When the connection opens successfully,
;; this channel then returns a two-way channel that you can use to communicate
;; with the web-socket server.

(defn test-ws []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws"))]
      (if-not error
        (do
          (>! ws-channel "Hello server from client!")
          (print (<! ws-channel)))
        
        (js/console.log "Error:" (pr-str error)))))
  )

(defn init! []
  (test-ws)
  )

(set! (.-onload js/window) (do (init!) ))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
