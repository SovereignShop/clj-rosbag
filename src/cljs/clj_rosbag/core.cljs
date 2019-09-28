(ns clj-rosbag.core
  (:require [promesa.core :as p]
            [cljs.core.async :refer [chan put!] :refer-macros [go]]
            [lz4js :as lz4]
            [rosbag :as Rosbag]
            [buffer :as Buffer]))

(defn- lz4-decompress [^js/Uint8Array buffer]
  (Buffer/Buffer. (lz4/decompress buffer)))

(defn- read-bag [ch bag]
  (.then bag (fn [b] (.readMessages b #js {:decompress #js {:lz4 lz4-decompress}}
                                    (fn [msg]
                                       (put! ch msg))))))

(defn open
  [bag-str]
  (let [size (.-length bag-str)
        char-codes (js/Array. size)
        _ (dotimes [i size]
            (aset char-codes i (.charCodeAt bag-str i)))
        byte-array (js/Uint8Array. char-codes)
        blob (js/Blob. #js[byte-array])]
    (Rosbag/open blob)))

(defn read-messages
  "Asyncronously reads messages into a channel."
  ([bag] (read-messages (chan)))
  ([bag ch compression]
   (read-bag ch bag)
   ch))
