(ns cam-client.input
  (:require
   [reagi.core :as r]
   [cam-client.utils :refer [scale-value str-float]]
   ;;[cam-client.physics :refer [move-to! move-right! move-left!]]
   ))

(def enable-snd (.getElementById js/document "sndShot"))
(def disable-snd (.getElementById js/document "sndReload"))

(defn play-snd [audio-elem]
  (if (aget audio-elem "paused") audio-elem)
  (.play audio-elem)
  (aset audio-elem "currentTime" 0)
  )


;;(play-snd enable-snd)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup controls

(def UP    38)
(def RIGHT 39)
(def LEFT  37)
(def SPACE 32)
(def PAUSE 80) ;; p

;;;; TODO Separate streams for keydowns and keyups

(defn keydown-stream []
  (let [out (r/events)]
    (set! (.-onkeydown js/document)
          #(r/deliver out [::down (.-keyCode %)]))
    out))

(defn keyup-stream []
  (let [out (r/events)]
    (set! (.-onkeyup js/document)
          #(r/deliver out [::up (.-keyCode %)]))
    out))

;;Merge key events into single event stream that
;;reduces active keys into set and gets updated every 25 ms.

(def active-keys-stream
  (->> (r/merge (keydown-stream) (keyup-stream))
       (r/reduce (fn [acc [event-type key-code]]
                   (condp = event-type
                     ::down (conj acc key-code)
                     ::up (disj acc key-code)
                     acc))
                 #{})
       (r/sample 25)))

;; (defn filter-map [pred f & args]
;;   "Helper function for responding to active keys with actions.
;;    Takes key type predicate and function with args thats evaluated.
;;    Eg: (filter-map #{RIGHT} move-right! pad)"
;;   (->> active-keys-stream
;;        (r/filter (partial some pred))
;;        (r/map (fn [_] (apply f args)))))

;; TODO pause cam
;; (->> active-keys-stream
;;      (r/filter (partial some #{PAUSE SPACE}))
;;      (r/throttle 100) ;; simple debounce
;;      (r/map #(cam-client.core/pause!))
;;      )

;;(filter-map #{RIGHT} move-right! pad)
;;(filter-map #{LEFT} move-left! pad)
;;(filter-map #{SPACE} move-ball! ball)


;;;; Mouse position

(defonce mouse-move-stream (r/events))

(defn start-mouse-listener []
  (println "Starting mouse listener")
  (play-snd enable-snd)
  (let [img-container js/document;; (.getElementById js/document "imgContainer")
        ]
    (set! (.-onmousemove img-container)
          #(r/deliver mouse-move-stream (.-clientX %)))))

(defn stop-mouse-listener []
  (println "Stopping mouse listener")
  (play-snd disable-snd)
  (let [img-container js/document]
    ;;(.removeEventListener img-container put-to-mouse-stream)
    (set! (.-onmousemove img-container) nil)))

;; Picture x-offset will be subtracted from read mouse x values.
;;TODO
;; (def mouse-x-offset
;;   (- (.ceil js/Math (.-left (.getBoundingClientRect
;;                              (.getElementById js/document "game")))) 1))

(def mouse-position-stream
  (->> mouse-move-stream
       (r/uniq) ;; Drop duplicate events
       ;; TODO scale mouse pos properly
       (r/map #(hash-map :scaled (->> (scale-value %
                                                   [0 (.-innerWidth js/window)]
                                                   [0 180])
                                      (.floor js/Math))
                         :unscaled (str-float % 2)))
       ))

;;;;;;;; Device Orientation

;; Capture deviceorientation event gamma values
(defonce orientation-change-stream (r/events))

(defn read-orientation-event [e]
  "Need to use named function to allow removing event listener."
  (r/deliver orientation-change-stream {:gamma (.-gamma e)
                                        :alpha (.-alpha e)
                                        :beta (.-beta e)
                                        }))
(defn start-orientation-listener []
  (println "Starting orientation listener")
  (play-snd enable-snd)
  (.addEventListener js/window "deviceorientation"
                     read-orientation-event false))

(defn stop-orientation-listener []
  (println "Stopping orientation listener")
  (play-snd disable-snd)
  (.removeEventListener js/window
                        "deviceorientation"
                        read-orientation-event false))

;; Cleanup and normalize orientation changes
;; TODO calibrate/optimize for different devices
;; -30 - 30 android phone
;; -15 - 15 macbook pro 2011
(defonce orientation-stream
  (->> orientation-change-stream
       (r/map :gamma)
       (r/uniq)
       (r/map #(do (print %) %))
       (r/map #(hash-map :scaled (->> (scale-value % [-30 30] [0 180])
                                      (.floor js/Math))
                         :unscaled (str-float % 2)))))

(defonce camera-rotation-stream
  (let [init-stream (r/events 90)] ;; Needs to be initialized for subscribers
    (->> (r/merge init-stream
                  (->> mouse-position-stream (r/map :scaled))
                  (->> orientation-stream (r/map :scaled)))
         ;; Subscribed in core.cljs
         )))
