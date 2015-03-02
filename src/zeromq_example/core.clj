(ns zeromq-example.core
  (:require [clojure.core.async :as async]
            [cheshire.core :as cheshire])
  (:import (org.zeromq ZContext ZMQ))
  (:import (java.util.zip Inflater)))

(defn market-data []
  (let [out (async/chan 1024)
        context (ZMQ/context 1)
        subscriber (.socket context ZMQ/SUB)]
    (.connect subscriber "tcp://relay-us-central-1.eve-emdr.com:8050")
    (.subscribe subscriber (byte-array 0))
    (async/thread
     (try
       (loop []
         (let [data (.recv subscriber)]
           (when true
             (do
               (async/>!! out data)
               (recur)))))
       (catch Throwable ex
         (println "Error occured: " ex)))
     (async/close! out)
     (println "Shutting down")
     (.term context))
    out))

(defn inflater [data]
  (let [inflater (Inflater.)
        decompressed (byte-array (* (alength data) 16))
        _ (.setInput inflater data)
        decompressed-size (.inflate inflater decompressed)
        output (byte-array decompressed-size)]
    (System/arraycopy decompressed 0 output 0 decompressed-size)
    (String. output "UTF-8")))


(let [c (market-data)]
  (for [row (:rowsets (cheshire/parse-string (inflater (async/<!! c)) true))]
    (for [r (:rows row)]
      (println r))))

(let [c (market-data)]
  (:columns (cheshire/parse-string (inflater (async/<!! c)) true)))



