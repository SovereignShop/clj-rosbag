(ns clj-rosbag.core
  (:require [clojure.java.data :refer [from-java]])
  (:import [com.github.swrirobotics.bags.reader BagFile]
           [com.github.swrirobotics.bags.reader.messages.serialization BagMessage MessageType ArrayType]))

(defrecord RosMessage [topic message time])

(declare msg-type->map)

(defmulti parse-field class)

(defmethod parse-field :default [field] (.getValue field))

(defmethod parse-field ArrayType
  [field]
  (case (.getType field)
    "int8[]"     (.getAsBytes field)
    "char[]"     (.getAsShorts field)
    "uint8[]"    (.getAsShorts field)
    "int16[]"    (.getAsShorts field)
    "uint16[]"   (.getAsInts field)
    "int32[]"    (.getAsInts field)
    "uint32[]"   (.getAsLongs field)
    "int64[]"    (.getAsLongs field)
    "uint64[]"   (.getAsBigIntegers field)
    "float32[]"  (.getAsFloats field)
    "float64[]"  (.getAsDoubles field)
    "time[]"     (.getAsTimestamps field)
    "duration[]" (.getAsDurations field)))

(defmethod parse-field MessageType
  [field]
  (msg-type->map field))

(defn msg-type->map
  ([msg-type]
   (persistent!
    (reduce (fn [ret field-name]
              (let [field (.getField msg-type field-name)
                    field-type (.getType field)]
                (assoc! ret (keyword field-name) (parse-field field))))
            (transient {})
            (.getFieldNames msg-type)))))

(defn from-bag-msg
  ([msg] (->RosMessage (.getTopic msg)
                       (.getMessage msg)
                       (.getTimestamp msg))))

(defn open
  "Open a bag. Can be a string representing a path to a bag file, or
  a byte array of the contents of a bag file."
  [bag]
  (doto (BagFile. bag) (.read)))

(defn read-messages
  "Read messages from bag."
  ([^BagFile bag]
   (read-messages bag (map #(.getName %) (.getTopics bag))))
  ([^BagFile bag topics]
   (map from-bag-msg (iterator-seq (.iterMessagesOnTopics bag (java.util.ArrayList. topics))))))

(comment
  (do
    (import '[java.io File])
    (def file (File. "resources/planar_lidar.bag"))
    (def bag (open (.getPath file)))
    (def msgs (read-messages bag))
    (def msg (:message (first msgs)))
    (def field-names (.getFieldNames msg))
    (def angle-increment (.getField msg "angle_increment"))
    (def angle-increment-type (.getType angle-increment))
    (def ranges (.getField msg "ranges"))
    (def ranges-type (.getType ranges))
    (msg-type->map msg)))
