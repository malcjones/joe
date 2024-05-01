(defproject joe "0.1.0-SNAPSHOT"
  :description "joe bookmark manager"
  :url "https://github.com/malcjones/joe"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [nrepl "1.1.0"]
                 [clojure-term-colors "0.1.0"]]
  :main ^:skip-aot joe.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
