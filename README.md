# clj-rosbag

Clojure(Script) ROS bag reader. It currently uses a forked version of the [Java Bag Reader](https://github.com/cartesian-theatrics/bag-reader-java) to support a Python-like API. Arbitrary ROS bag mesasges are
read into clojure data structures. Note that primitive array types are read as the corresponding Java array type (not clojure vectors).

# Usage

```clojure
(import '[java.io File])
(require '[clj-rosbag.core :as rosbag])
(def bag-file (File. "resources/Float32.bag"))
(def bag (rosbag/open (.getPath bag-file)))

;; Returns a lazy sequence of messages on topic "/data".
(def messages (rosbag/read-messages bag ["/data"]))
```
