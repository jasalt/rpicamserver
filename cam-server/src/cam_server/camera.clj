;; Interface for webcamera v4l4j
;; See https://github.com/alandipert/berrycam
;; http://badankles.com/?p=209
(ns cam-server.camera
  (:require ;;[base64-clj.core :as base64]
   [clojure.string  :as s]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io])
  ;; (:use [clojure.java.shell :only [sh]])
  (:import
   (com.github.sarxos.webcam Webcam)
   (java.awt.image BufferedImage)
   (javax.imageio ImageIO)
   (java.io ByteArrayOutputStream)
   )
  )

(def default-device "/dev/video0")


(defn to-base64-img-src [img-byte-array]
  (->> img-byte-array
       b64/encode
       (map char)
       (apply str)
       (str "data:image/jpeg;base64,")
       ))

;;(def webcam (Webcam/getDefault))
(let [cams (Webcam/getWebcams 4000)]
  (if (< 1 (count cams))
    (def webcam (second cams))    
    (def webcam (first cams))))

;;(.close webcam)
;;(bean webcam)
(defn take-b64-pic! []
  "Get b64 encoded jpeg image from /dev/video0 with resolution 320x240."
  (when-not (get (bean webcam) :open)
    (.open webcam))
  
  (let [buff-img (.getImage webcam)
        baos (ByteArrayOutputStream.)]
    (ImageIO/write buff-img "jpg" baos)
    (to-base64-img-src (.toByteArray baos))
    )
  )

(comment
  ;; This could be removed or used for timelapses maybe?
  (defn take-picture-fswebcam [[width height] & device]
    "Take picture with fswebcam and encode it with base64. On laptop, one picture
   takes ~500ms to take. Device defaults to /dev/video0."
    (let [dev (or device default-device)
          size (str width "x" height)
          args ["fswebcam" "-S" "2" "-d" dev
                "-r" size "--jpeg" "80" "--save" "-"]]
      (-> (apply sh args)
          :out))))
