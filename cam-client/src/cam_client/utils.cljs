(ns cam-client.utils
  (:require [cljs.pprint :refer [pprint]]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [chan close!]]
            )
  (:require-macros
   [cljs.core.async.macros :as m :refer [go]])
  )

(defn log [msg]
  (.log js/console (pprint msg))
  )

(defn str-float
  "Convert float to str rounded to n decimals (default 1)."
  ([x n]
   (if x (gstring/format (str "%."n"f") x)))
  ([x] (str-float x 1))
  )

(defn scale-value [x [x-min x-max] [to-min to-max]]
  "Scale given value thats between x-min and x-max to range to-min to-max.
   TODO bug-ridden"
  (let [portion (/ (.abs js/Math (- x x-min)) (- x-max x-min))] 
    (+ to-min (* portion (- to-max to-min)))))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))
