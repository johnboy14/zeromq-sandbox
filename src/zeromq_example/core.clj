(ns zeromq-example.core
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io])
  (:import (org.zeromq ZMQ))
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
  (when data
    (let [inflater (Inflater.)
          decompressed (byte-array (* (alength data) 16))
          _ (.setInput inflater data)
          decompressed-size (.inflate inflater decompressed)
          output (byte-array decompressed-size)]
      (System/arraycopy decompressed 0 output 0 decompressed-size)
      (String. output "UTF-8"))))

(defn process-market-data [m-chan o-chan]
  (async/thread
    (loop []
      (if-let [c (inflater (async/<!! m-chan))]
        (do (async/>!! o-chan c)
            (recur))
        (println "Market data channel has been closed!!")))))


(defn write-to-file [o-chan filename]
  (async/thread
    (with-open [wrt (io/writer filename)]
      (loop []
        (if-let [d (async/<!! o-chan)]
          (do (.write wrt d)
              (recur))
          (println "Order Channel has been closed"))))))

(defn start [m-chan o-chan]
  (process-market-data m-chan o-chan)
  (write-to-file o-chan "/tmp/data.txt"))

(defn stop [m-chan o-chan]
  (async/close! m-chan)
  (async/close! o-chan))

(comment "Usage"
  ;;channel for market feed
  (def market-chan (market-data))
  ;;file write channel
  (def writer-chan (async/chan 1024))

  ;;start streaming market data and writing to file
  (start market-chan order-chan)

  ;;might take a while to stop because the channel might contain data that must be consumed first
  (stop market-chan order-chan)

  ;; both channels should return nil values after they have been emptied
  (async/<!! market-chan)
  (async/<!! order-chan))


