(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'cam-client.core
   :output-to "out/cam_client.js"
   :output-dir "out"})
