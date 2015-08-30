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
         ;;(reset! sensor-state sensor-readings)
         )
       (recur)))))

(defn receive-fn [serial-input-chan]
  "Reads input bytes and puts them to queue."
  (fn [b]
    (>!! serial-input-chan (.read b))))



(defn initialize []
  ;; TODO read and set port automagically
  (defonce arduino-port (serial/open "ttyACM0" :baud-rate 9600))  
  ;; (list-ports)

  (def serial-input-chan (chan))
  (def pchan (print-messages serial-input-chan))
  (def rfn (receive-fn serial-input-chan))
  (serial/listen arduino-port rfn nil)
  arduino-port)

(defn cleanup [port]
  (serial/remove-listener port)
  (serial/close port)
  )

(extend-protocol serial/Bytable
  String
  (to-bytes [this] (.getBytes this "ASCII")))

;;(defonce arduino-port (initialize))

(defn servo-to [degree]
  "Move servo to given degree."
  ;;(println ("Servo t: " degree))
  (->> degree char str
       (serial/write arduino-port)) ;; TODO dumb conversions
  )

(comment
  (def sp (initialize))
  (servo-to 3)
  (cleanup arduino-port)
  )
