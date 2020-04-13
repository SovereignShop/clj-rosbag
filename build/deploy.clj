(ns deploy
  (:require
   [badigeon.compile :as compile]
   [badigeon.prompt :as prompt]
   [badigeon.sign :as sign]
   [badigeon.deploy :as deploy]
   [badigeon.jar :as jar]
   [clojure.tools.deps.alpha.reader :as deps-reader]))

(defn deploy-lib []
  (let [deps (:deps (deps-reader/slurp-deps "deps.edn"))]
    (jar/jar 'org.cartesiantheatrics/clj-rosbag {:mvn/version "0.0.1"}
             {:out-path                "target/clj-rosbag-0.0.1.jar"
              :paths                   ["src/clj"]
              :deps                    deps
              :mvn/repos               '{"clojars" {:url "https://repo.clojars.org/"}}
              :exclusion-predicate     jar/default-exclusion-predicate
              :allow-all-dependencies? true})
    (let [artifacts (-> [{:file-path "target/clj-rosbag-0.0.1.jar"
                          :extension "jar"}
                         {:file-path "pom.xml"
                          :extension "pom"}]
                        (badigeon.sign/sign {:command "gpg"}))
          password  (badigeon.prompt/prompt-password "Password: ")]
      (badigeon.deploy/deploy 'org.cartesiantheatrics/clj-rosbag
                              "0.0.1"
                              artifacts
                              {:url "https://repo.clojars.org/"}
                              {:credentials     {:username "cartesiantheatrics"
                                                 :password password}
                               :allow-unsigned? true}))))

(defn -main []
  (deploy-lib))
