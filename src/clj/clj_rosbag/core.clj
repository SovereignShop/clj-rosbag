(ns clj-rosbag.core
  (:require [clojure.java.data :refer [from-java]])
  (:import [com.github.swrirobotics.bags.reader BagFile]
           [com.github.swrirobotics.bags.reader.messages.serialization BagMessage MessageType]))

(defrecord RosMessage [topic msg time])

#_(defn parse-field [field-type field]
  (case field-type
    "bool" (+ 1)
    "bool[]"
    "int8"
    "int8[]"
    "uint8"
    "int16"
    "uint16"
    "int32"
    "uint32"
    "int64"
    "uint64"
    "float32"
    "float64"
    "string"
    "time"
    "duration"))

#_(defmethod from-java MessageType
  ([msg-type]
   (loop [[field-names & field-names] (.getFieldNames msg-type) ret (transient {})]
     (let [field (.getField msg-type field-name)
           field-type (.getType field)]
       (if (nil? field-names)
         (persistent! ret)
         (recur field-names (assoc! ret (keyword field-name) (parse-field field-type field))))))))

#_(defmethod from-java BagMessage
  ([msg] (->RosMessage (.-topic msg)
                       (from-java (.-msg msg))
                       (.-timestamp msg))))

(defn open
  "Open a bag. Can be a string representing a path to a bag file, or
  a byte array of the contents of a bag file."
  [bag]
  (doto (BagFile. bag) (.read)))

(defn read-messages
  ""
  ([^BagFile bag]
   (read-messages bag (map #(.getName %) (.getTopics bag))))
  ([^BagFile bag topics]
   (iterator-seq (.iterMessagesOnTopics bag (java.util.ArrayList. topics)))))

(comment
  (do
    (import '[java.io File])
    (def file (File. "resources/planar_lidar.bag"))
    (def bag (open (.getPath file)))
    (first msgs) 
    (def msgs (read-messages bag))))
