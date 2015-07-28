(ns cam-server.servo
  (:require
   [clojure.core.async :as a :refer [go chan <! >! <!! >!!]]
   [serial.core :as serial]
   [serial.util :refer [list-ports]]
   )
  )


(defn print-messages
  "Prints incoming messages type from from-chan."
  ([from-chan]
   (go
     (loop []
       (let [sensor-readings (<! from-chan)]
         (println (str "Got: " sensor-readings))
         (reset! sensor-state sensor-readings)
         )
       (recur)))))

(defn receive-fn [serial-input-chan]
  "Reads input bytes and puts them to queue."
  (fn [b]
    (>!! serial-input-chan (.read b))))

(def port (serial/open "cu.usbmodemfa131" :baud-rate 9600))

(defn send-command [port command]
  "Sends command to serial port."
  ;; TODO: this probably doesn't work
  (serial/write port command))

(defn initialize []
  ;; TODO read and set port automagically
  (def serial-input-chan (chan))
  (def pchan (print-messages serial-input-chan))
  (def rfn (receive-fn serial-input-chan))
  (serial/listen port rfn nil)
  port)

(defn cleanup [port]
  (serial/remove-listener port)
  (serial/close port)
  )

(defn servo-to [degree]
  (send-command (char degree)))
