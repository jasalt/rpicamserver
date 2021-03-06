(defproject cam-server "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.8.5"]
                 [com.taoensso/timbre "4.0.2"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.67"]
                 [environ "1.0.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-session-timeout "0.1.0"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.3"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [http-kit "2.1.19"]
                 
                 [jarohen/chord "0.6.0"]
                 [danlentz/clj-uuid "0.1.6"]
                 [org.clojure/data.codec "0.1.0"]

                 ;; [com.github.sarxos/webcam-capture-driver-v4l4j
                 ;;  "0.3.11-SNAPSHOT"]
                 [com.github.sarxos/webcam-capture "0.3.11-SNAPSHOT"]
                 [com.github.sarxos/v4l4j "0.9.1-r507"]
                 [clj-serial "2.0.3"]]
  :repositories {"sonatype snapshots"
                 "http://oss.sonatype.org/content/repositories/snapshots"}
  :min-lein-version "2.0.0"
  :uberjar-name "cam-server.jar"
  :jvm-opts ["-server"]

  :main cam-server.core

  :plugins [[lein-environ "1.0.0"]
            [lein-ancient "0.6.5"]]
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :aot :all}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[ring/ring-mock "0.2.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.0"]]
                  
                  
                  :repl-options {:init-ns cam-server.core}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
