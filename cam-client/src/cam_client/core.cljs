(ns ^:figwheel-always cam-client.core
    (:require
     [cam-client.config]
     [cam-client.input]
     ;;[cam-client.game :refer [game-canvas ball-entity pad-entity]]
     ))

(enable-console-print!)

(defn init! []
  ;; Start ajax-ing
  nil
  )

(set! (.-onload js/window) init!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

