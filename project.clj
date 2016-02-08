(def version (clojure.string/trim-newline (slurp "VERSION")))

(defproject io.framed/external version
  :description "Constant-space algorithms and data structures for Clojure"
  :url "https://github.com/framed-data/external"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/framed-data/external"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.avl "0.0.12"]
                 [org.clojure/data.fressian "0.2.1"]
                 [org.clojure/test.check "0.8.0"]
                 [factual/riffle "0.1.3"]
                 [tesser.core "1.0.1"]
                 [io.framed/std "0.2.1"]]
  :plugins [[codox "0.8.13"]]
  :codox {:output-dir "doc"})
