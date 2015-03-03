(ns zeromq-example.core
  (:require [clojure.core.async :as async]
            [cheshire.core :as cheshire]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojure.java.io :as io])
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

(def order-chan (async/chan 1024))

(defn process-market-data []
  (let [c (market-data)]
    (async/go-loop []
      (async/>!! order-chan (inflater (async/<!! c)))
      (recur))))

(defn persist-data []
  (let [connection (esr/connect)]
    (async/go-loop []
      (esd/create connection "emdr" "orders" (async/<!! order-chan)))))


(defn write-thousand-lines [filename]
  (with-open [wrt (io/writer filename)]
    (dotimes [_ 1000]
      (.write wrt (async/<!! order-chan)))))

;(write-thousand-lines "/Users/e20042/data.txt")

(defn start []
  (process-market-data)
  ;(persist-data)
  (write-thousand-lines "/Users/e20042/data.txt"))



