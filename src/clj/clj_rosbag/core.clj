(ns clj-rosbag.core
  (:import [com.github.swrirobotics.bags.reader BagFile TopicInfo]
           [com.github.swrirobotics.bags.reader.messages.serialization BagMessage MessageType ArrayType]))

(defrecord RosMessage [topic message time])

(declare msg-type->map)

(defmulti parse-field class)

(defmethod parse-field :default [field] (.getValue field))

(defmethod parse-field ArrayType
  [^ArrayType field]
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
    "duration[]" (.getAsDurations field)
    (mapv parse-field (.getFields field))))

(defmethod parse-field MessageType
  [field]
  (msg-type->map field))

(defn msg-type->map
  ([^MessageType msg-type]
   (persistent!
    (reduce (fn [ret field-name]
              (let [field (.getField msg-type field-name)]
                (assoc! ret (keyword field-name) (parse-field field))))
            (transient {})
            (.getFieldNames msg-type)))))

(defn from-bag-msg
  ([^BagMessage msg] (->RosMessage (.getTopic msg)
                                   (msg-type->map (.getMessage msg))
                                   (.getTimestamp msg))))

(defn open
  "Open a bag. Can be a string representing a path to a bag file, or
  a byte array of the contents of a bag file."
  [bag]
  (doto (BagFile. bag) (.read)))

(defn read-messages
  "Read messages from bag."
  ([^BagFile bag]
   (read-messages bag (map (fn [^TopicInfo topics] (.getName topics)) (.getTopics bag))))
  ([^BagFile bag topics]
   (map from-bag-msg (iterator-seq (.iterMessagesOnTopics bag (java.util.ArrayList. topics))))))

(comment
  (do
    (import '[java.io File]) 
    (def file (File. "resources/planar_lidar.bag"))
    (def bag (open  (type (.getPath file))))
    (def msgs (read-messages bag))
    (-> msgs first :message :angle_min)
    (def msg (:message (first msgs)))))
