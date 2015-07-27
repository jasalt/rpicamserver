;; Interface for webcamera with fswebcam.
;; TODO use v4l4j etc
;; See https://github.com/alandipert/berrycam
;; http://badankles.com/?p=209
(ns cam-server.camera
  (:require ;;[base64-clj.core :as base64]
   [clojure.string  :as s]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io])
  (:use [clojure.java.shell :only [sh]])
  (:import (au.edu.jcu.v4l4j CaptureCallback FrameGrabber ImageFormat
                             JPEGFrameGrabber V4L4JConstants VideoDevice
                             VideoFrame)
           (au.edu.jcu.v4l4j.exceptions V4L4JException)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io ByteArrayOutputStream)
           )
  )

(def default-device "/dev/video0")

(def captures (atom {}))

(defn ^JPEGFrameGrabber jpeg-grabber
  [^VideoDevice vd opts]
  (let [{:keys [width height quality]} opts]
    (.getJPEGFrameGrabber vd
                          width
                          height
                          0
                          V4L4JConstants/STANDARD_WEBCAM
                          quality)))

(defn time-now [] (System/currentTimeMillis))

(defmacro with-frame [frame & body]
  `(let [res# (do ~@body)]
     (do (.recycle ~frame)
         res#)))

(defn next-frame-action
  [{:keys [last-time] :as last-capture}, ^VideoFrame next-frame, max-interval]
  (let [interval (- (time-now) last-time)]
    (with-frame next-frame
      (if (> interval max-interval)
        (let [buf (.getBufferedImage next-frame)
              len (.getFrameLength next-frame)]
          {:buf buf :len len :last-time (time-now)})
        last-capture))))

(defn init-capture
  [device-path opts]
  (let [{:keys [max-interval-ms]} opts
        latest (promise)
        agt (agent {:buf nil, :len nil, :last-time 0})
        vd (VideoDevice. device-path)
        fg (doto (jpeg-grabber vd opts)
             (.setCaptureCallback
              (reify CaptureCallback
                (^void exceptionReceived [_ ^V4L4JException e])
                (^void nextFrame [_ ^VideoFrame frame]
                  (send-off agt
                            next-frame-action
                            frame
                            max-interval-ms)
                  (when-not (realized? latest)
                    (await agt)
                    (deliver latest agt))))))]
    (.startCapture fg)
    (swap! captures assoc device-path {:vd vd :fg fg :latest latest})))

(def capture-defaults
  {:width 320
   :height 240
   :quality 60
   :max-interval-ms 5000})

(defn capture!
  [device-path & opt-pairs]
  "Returns an agent containing the last capture as a BufferedImage,
  initializing the device if necessary. Takes optional parameters:
  :width - The width of the frame to grab. (default 320)
  :height - The height of the frame to grab. (default 240)
  :quality - The JPEG quality. (default 60)
  :max-interval-ms - The maximum interval, in ms, between captures. (default 5000)"
  (if-let [capture (get @captures device-path)]
    @(:latest capture)
    (let [opts (merge capture-defaults (apply hash-map opt-pairs))]
      (println opts)
      (init-capture device-path opts)
      (capture! device-path))))

(defn stop!
  [device-path]
  (let [{:keys [vd fg]} (get @captures device-path)]
    (swap! captures dissoc device-path)
    (.stopCapture ^FrameGrabber fg)
    (.releaseFrameGrabber ^VideoDevice vd)
    (.release ^VideoDevice vd)))

(defn stop-all! []
  (doseq [device-path (keys @captures)]
    (stop! device-path)))

;;;;;;;;;
;;;;;;;;;

(defn take-pic! [[[width height] & device]]
  @(capture! (or device default-device)
             :width width
             :height height
             :max-interval-ms 5000
             :quality 60))

(defn to-base64-img-src [img-byte-array]
  (->> img-byte-array
       b64/encode
       (map char)
       (apply str)
       (str "data:image/jpeg;base64,")
       ))

(defn take-b64-pic! []
  "Get b64 encoded jpeg image from /dev/video0 with resolution 320x240."
  (let [buff-img ((take-pic! [320 240] default-device) :buf)
        baos (ByteArrayOutputStream.)]
    (ImageIO/write buff-img "jpg" baos)
    ;; (catch Exception e (str "Exception in jpg conversion: " (.getMessage e)))
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
