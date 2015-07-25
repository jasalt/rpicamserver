;; Use DOM for showing game state notifications and debug info.
(ns cam-client.hud
  (:require
   [reagi.core :as r]
   [reagent.core :as dom]
   [cam-client.input :as input]
   [cam-client.config :refer [config set-input]]
   ;;[cam-client.utils :refer [timeout]]
   )
  )
 
(defonce hud-state
  (dom/atom
   {:accelerometer "[Waiting for input values]"}))

;; UI atom gets updated by stream that is thorttled.
(defonce hud-update-stream
  (let [in-stream (r/events) ;; Entity values are delivered to this stream
        mouse-stream (->> input/mouse-position-stream
                          (r/map #(hash-map :mouse %)))
        orientation-stream (->> input/orientation-stream
                                (r/map #(hash-map :orientation %)))]
    (->> (r/merge mouse-stream orientation-stream in-stream)
         (r/reduce (fn [coll event] (merge coll event)))
         (r/sample 50)
         (r/map #(reset! hud-state %)))
    in-stream))

(defn debug-view []
  [:div
   [:h2 "Input Configuration"]
   [:p "Click Jack to toggle"]
   
   (let [mouse-state (-> @config :input :mouse :active)]
     [:input { :type "checkbox" :checked mouse-state
              :on-change #(set-input :mouse (not mouse-state))}])
   [:p 
    " Mouse X " (:mouse @hud-state)]
   [:br]
   (let [orientation-state (-> @config :input :orientation :active)]
     [:input {:type "checkbox" :checked orientation-state
              :on-change #(set-input :orientation (not orientation-state))}])
   [:p " Orientation "
    (when-let [x-val (:orientation @hud-state)]
      (str "X " (:scaled x-val) " Raw " (:unscaled (:orientation @hud-state))))]

   [:h2 "Camera sensors:"]
   [:p "TODO" ]
   ])

(defn test-view []
  [:div
   [:p "Test view!"]
   ])

;; (defn tell-hud [msg]
;;   "Tell hud about some event."
;;   (r/deliver hud-update-stream msg))

(dom/render-component
 [debug-view]
 (.getElementById js/document "hudContainer"))
