;; Interface for webcamera with fswebcam.
;; TODO use v4l4j etc
;; See https://github.com/alandipert/berrycam
(ns cam-server.camera
  (:require [base64-clj.core :as base64])
  (:use [clojure.java.shell :only [sh]]))

(def default-device "/dev/video0")

(defn take-picture [[width height] & device]
  "Take picture with fswebcam and encode it with base64. On laptop, one picture
   takes ~500ms to take. Device defaults to /dev/video0."
  (let [dev (or device default-device)
        size (str width "x" height)
        args ["fswebcam" "-S" "2" "-d" dev
              "-r" size "--jpeg" "80" "--save" "-"]]
    (-> (apply sh args) :out base64/encode)))
