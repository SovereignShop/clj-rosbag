(ns clj-rosbag.core
  (:import [com.github.swrirobotics.bags.reader BagFile TopicInfo]
           [java.sql Timestamp]
           [com.github.swrirobotics.bags.reader.messages.serialization
            UInt64Type UInt32Type UInt16Type UInt8Type Float32Type Float64Type StringType
            Int8Type Int16Type Int32Type Int64Type TimeType DurationType BoolType BagMessage MessageType ArrayType]))

(defrecord RosMessage [^String topic ^MessageType message ^Timestamp time])

(defprotocol RosMessageField
  (parse-field [field]))

(declare msg-type->map)

(extend-protocol RosMessageField
  UInt32Type
  (^Long parse-field [this] (.getValue this))
  UInt64Type
  (^BigInteger parse-field [this] (.getValue this))
  UInt16Type
  (^Integer parse-field [this] (.getValue this))
  UInt8Type
  (^Short parse-field [this] (.getValue this))
  Float32Type
  (^Flaot parse-field [this] (.getValue this))
  Float64Type
  (^Double parse-field [this] (.getValue this))
  StringType
  (^String parse-field [this] (.getValue this))
  Int8Type
  (^Byte parse-field [this] (.getValue this))
  Int16Type
  (^Short parse-field [this] (.getValue this))
  Int32Type
  (^Integer parse-field [this] (.getValue this))
  Int64Type
  (^Double parse-field [this] (.getValue this))
  TimeType
  (^Timestamp parse-field [this] (.getValue this))
  DurationType
  (^Double parse-field [this] (.getValue this))
  BoolType
  (^Boolean parse-field [this] (.getValue this))
  MessageType
  (parse-field [this] (msg-type->map this))
  ArrayType
  (parse-field [this]
    (case (.getType this)
      "int8[]"     (.getAsBytes this)
      "char[]"     (.getAsShorts this)
      "uint8[]"    (.getAsShorts this)
      "int16[]"    (.getAsShorts this)
      "uint16[]"   (.getAsInts this)
      "int32[]"    (.getAsInts this)
      "uint32[]"   (.getAsLongs this)
      "int64[]"    (.getAsLongs this)
      "uint64[]"   (.getAsBigIntegers this)
      "float32[]"  (.getAsFloats this)
      "float64[]"  (.getAsDoubles this)
      "time[]"     (.getAsTimestamps this)
      "duration[]" (.getAsDurations this)
      (mapv parse-field (.getFields this)))))

(defn msg-type->map
  ([^MessageType msg-type]
   (persistent!
    (reduce (fn [ret field-name]
              (let [field (.getField msg-type field-name)]
                (assoc! ret (keyword field-name) (parse-field field))))
            (transient {})
            (.getFieldNames msg-type)))))

(defn from-bag-msg
  ([^BagMessage msg]
   (->RosMessage (.getTopic msg)
                 (msg-type->map (.getMessage msg))
                 (.getTimestamp msg))))

(defn open
  "Open a bag. Can be a string representing a path to a bag file, or
  a byte array of the contents of a bag file."
  [bag]
  (doto (BagFile. bag) (.read)))

(defn get-info
  [^BagFile bag]
  {:compresion-type (.getCompressionType bag)
   :message-count (.getMessageCount bag)
   :is-indexed (.isIndexed bag)
   :topic-info (vec (for [^TopicInfo topic (.getTopics bag)]
                      {:name (.getName topic)
                       :message-count (.getMessageCount topic)
                       :message-type (.getMessageType topic)
                       :md5sum (.getMessageMd5Sum topic)
                       :connection-count (.getConnectionCount topic)}))})

(defn read-messages
  "Read messages from bag."
  ([^BagFile bag]
   (read-messages bag (map (fn [^TopicInfo topics] (.getName topics)) (.getTopics bag))))
  ([^BagFile bag topics]
   (map from-bag-msg (iterator-seq (.iterMessagesOnTopics bag (java.util.ArrayList. topics))))))

(defn rosbag? [x]
  (instance? BagFile x))

(comment
  (do
    (import '[java.io File])
    (def file (File. "resources/planar_lidar.bag"))
    (def bag (open  (type (.getPath file))))
    (def msgs (read-messages bag))
    (-> msgs first :message :angle_min)
    (def msg (:message (first msgs)))))
