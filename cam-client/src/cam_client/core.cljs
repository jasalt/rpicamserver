(ns ^:figwheel-always cam-client.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [cam-client.config]
     [cam-client.input]
     [cam-client.hud]

     [taoensso.sente :as ws]
     [taoensso.sente.packers.transit :as sente-transit]
     
     [cljs.core.async :as async :refer [<! >! chan]]
     [reagent.core :as dom]
     ;;[cam-client.game :refer [game-canvas ball-entity pad-entity]]
     ))

(enable-console-print!)

;; (sente/set-logging-level! :trace) ; Uncomment for more logging
;;;; Packer (client<->server serializtion format) config
(def packer (sente-transit/get-flexi-packer :edn))



(defonce app-state
  "Our very minimal application state - a piece of text that we display."
  (dom/atom
   {:data/text "Enter a string and press RETURN!"}))

(defn set-state [state-key val]
  (swap! app-state merge {state-key val}))

(defn init! []
  (let [{:keys [chsk ch-recv send-fn state]}
        (ws/make-channel-socket! "/chsk" {:type :auto
                                          :packer packer})]
    (def chsk       chsk)
    (def ch-chsk    ch-recv)
    (def chsk-send! send-fn)
    (def chsk-state chsk-state))
  )

(defmulti event-msg-handler :id) ; Dispatch on event-id
;; Wrap for logging, catching, etc.:
(defn     event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(do ; Client-side methods
  (defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event]}]
    (debugf "Unhandled event: %s" event))

  (defmethod event-msg-handler :chsk/state
    [{:as ev-msg :keys [?data]}]
    (if (= ?data {:first-open? true})
      (debugf "Channel socket successfully established!")
      (debugf "Channel socket state change: %s" ?data)))

  (defmethod event-msg-handler :chsk/recv
    [{:as ev-msg :keys [?data]}]
    (debugf "Push event from server: %s" ?data))

  (defmethod event-msg-handler :chsk/handshake
    [{:as ev-msg :keys [?data]}]
    (let [[?uid ?csrf-token ?handshake-data] ?data]
      (debugf "Handshake: %s" ?data)))

  ;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...
  )


(def router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (ws/start-chsk-router! ch-chsk event-msg-handler*)))

(set! (.-onload js/window) (do
                             (init!)
                             (start-router!)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
