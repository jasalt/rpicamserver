(ns cam-client.config
  (:require
   [reagent.core :as r]))

(def default-inputs [:mouse
                     ])

(def initial-config
  {:input {:mouse
           {:active false
            :toggle-fn (fn [new-state]
                         (if new-state
                           (cam-client.input/start-mouse-listener)
                           (cam-client.input/stop-mouse-listener)
                           ))}
           :orientation
           {:active false
            :toggle-fn (fn [new-state]
                         (if new-state
                           (cam-client.input/start-orientation-listener)
                           (cam-client.input/stop-orientation-listener)
                           ))}}
   })

(defonce config (r/atom initial-config))

;; Control game config from dom

(defn set-config [new-state & path]
  "Set config value to new state."
  (let [path-vec (into [] path)] (swap! config assoc-in path-vec new-state)))

(defn set-input [input-type new-state]
  "Input type is :mouse or :orientation. Runs side effective toggle-fn for given
   input and changes it's config atom value that's connected to view."
  (let [toggle-fn (-> @config :input input-type :toggle-fn)]
    (set-config new-state :input input-type :active)
    (toggle-fn new-state)))

(defn set-initial-inputs! []
  (doseq [input default-inputs]
    (if-not (-> @config :input input :active)
      (set-input input true)
      (print (str input" already active, skipping"))))) 

;; Hacky function call for setting initial inputs
(set! (.-onload js/window) (js/setTimeout set-initial-inputs! 2000))

