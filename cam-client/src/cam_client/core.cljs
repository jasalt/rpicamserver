(ns ^:figwheel-always cam-client.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require
     [cam-client.input]
     [cam-client.config]
     [cam-client.hud]
     [cljs.reader :as edn]
     [cljs.core.async :as async :refer [<! >! chan put!]]
     [chord.client :refer [ws-ch]]
     [reagent.core :as dom]
     [reagi.core :as r]
     ;;[cam-client.game :refer [game-canvas ball-entity pad-entity]]
     ))

(enable-console-print!)

(defn message-box [new-msg-ch]
  (let [!input-value (doto (dom/atom nil)
                       (->> (set! js/window.input-value)))]
    (fn []
      [:div
       [:h3 "Send a message to the server:"]
       [:input {:type "text",
                :size 50,
                :autofocus true
                :value @!input-value
                :on-change (fn [e]
                             (reset! !input-value (.-value (.-target e))))

                :on-key-press (fn [e]
                                (when (= 13 (.-charCode e))
                                  (put! new-msg-ch @!input-value)
                                  (reset! !input-value "")))}]])))

(defn message-list [!msgs]
  [:div
   [:h3 "Messages from the server:"]
   [:ul
    (if-let [msgs (seq @!msgs)]
      (for [msg msgs]
        ^{:key msg} [:li (pr-str msg)])

      [:li "None yet."])]])

(defn message-component [!msgs new-msg-ch]
  [:div
   [message-box new-msg-ch]
   [message-list !msgs]])


(defn add-msg [msgs new-msg]
  ;; we keep the most recent 10 messages
  (->> (cons new-msg msgs)
       (take 10)))

(defn receive-msgs! [!msgs server-ch]
  ;; every time we get a message from the server, add it to our list
  (go-loop []
    (when-let [msg (<! server-ch)]
      (swap! !msgs add-msg msg)
      (recur))))

(defn send-msgs! [new-msg-ch server-ch]
  ;; send all the messages to the server
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (>! server-ch msg)
      (recur))))

(defn init! []
  ;;(test-ws)
  (go
    (let [container (.getElementById js/document "msgContainer")
          {:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws"
                                                      {:format :transit-json}))]
            (if error
              ;; connection failed, print error
              (dom/render-component
               [:div
                "Couldn't connect to websocket: "
                (pr-str error)]
               container)

              (let [ ;; !msgs is a shared atom between the model (above,
                    ;; handling the WS connection) and the view
                    ;; (message-component, handling how it's rendered)
                    !msgs (doto (dom/atom [])
                            (receive-msgs! ws-channel))

                    ;; new-msg-ch is the feedback loop from the view -
                    ;; any messages that the view puts on here are to
                    ;; be sent to the server
                    new-msg-ch (doto (chan 1)
                                 (send-msgs! ws-channel))]
                
                ;; subscribe to event stream
                (r/subscribe cam-client.input/camera-rotation-stream
                             new-msg-ch)
                
                ;; show the message component
                (dom/render-component
                 [message-component !msgs new-msg-ch]
                 container))))))

(set! (.-onload js/window) (do (init!) ))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
