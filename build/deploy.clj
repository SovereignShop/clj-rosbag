(ns deploy
  (:require
   [badigeon.exec :as exec]
   [badigeon.prompt :as prompt]
   [badigeon.sign :as sign]
   [badigeon.deploy :as deploy]
   [badigeon.jar :as jar]
   [clojure.string :as s]
   [clojure.tools.deps.alpha.reader :as deps-reader]))

(defn deploy-lib []
  (let [deps    (:deps (deps-reader/slurp-deps "deps.edn"))
        version (s/trim (with-out-str
                          (exec/exec "git" {:proc-args ["describe" "--tags"]
                                            ;; The error message of the exception thrown upon error.
                                            :error-msg "Failed to get tags"})))]
    (assert (re-find #"^\d.\d.\d$" version))
    (jar/jar 'org.cartesiantheatrics/clj-rosbag {:mvn/version version}
             {:out-path                (format "target/clj-rosbag-%s.jar" version)
              :paths                   ["src/clj"]
              :deps                    deps
              :mvn/repos               '{"clojars" {:url "https://repo.clojars.org/"}}
              :exclusion-predicate     jar/default-exclusion-predicate
              :allow-all-dependencies? true})
    (let [artifacts (-> [{:file-path (format "target/clj-rosbag-%s.jar" version)
                          :extension "jar"}
                         {:file-path "pom.xml"
                          :extension "pom"}]
                        (badigeon.sign/sign {:command "gpg"}))
          password  (badigeon.prompt/prompt-password "Password: ")]
      (badigeon.deploy/deploy 'org.cartesiantheatrics/clj-rosbag
                              version
                              artifacts
                              {:url "https://repo.clojars.org/"}
                              {:credentials     {:username "cartesiantheatrics"
                                                 :password password}
                               :allow-unsigned? true}))))

(defn -main []
  (deploy-lib))
