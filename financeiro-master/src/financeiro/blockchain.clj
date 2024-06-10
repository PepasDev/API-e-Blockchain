(ns financeiro.blockchain
  (:require [clojure.data.json :as json])
  (:import [java.security MessageDigest]
           [java.util Date])
  (:gen-class))

;; Função para calcular o hash SHA-256 a partir de uma entrada
(defn sha256-hash [input]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest digest (.getBytes input "UTF-8"))))))

;; Função para calcular o hash do bloco
(defn compute-block-hash [index nonce data previous-hash]
  (sha256-hash (str index nonce data previous-hash)))

;; Função para criar um novo bloco
(defn new-block [index nonce data previous-hash hash]
  {:index index
   :nonce nonce
   :data data
   :previous-hash previous-hash
   :hash hash})

;; Função para minerar um novo bloco
(defn mine-block [index data previous-hash]
  (loop [nonce 0]
    (let [hash (compute-block-hash index nonce data previous-hash)]
      (if (.startsWith hash "0000")
        (new-block index nonce data previous-hash hash)
        (recur (inc nonce))))))

;; Função para criar o bloco gênesis
(defn create-genesis-block []
  (mine-block 0 "genesis-block" "0000000000000000000000000000000000000000000000000000000000000000"))

;; Átomo para armazenar a blockchain
(def blockchain (atom [(create-genesis-block)]))

;; Função para adicionar um novo bloco à blockchain
(defn add-block [data]
  (let [last-block (last @blockchain)
        new-index (inc (:index last-block))
        new-block (mine-block new-index data (:hash last-block))]
    (swap! blockchain conj new-block)))

;; Função principal
(defn -main
  "Função principal da aplicação"
  [& args]
  (println @blockchain)
  ;; Adicionar blocos de teste
  (add-block "data1")
  (add-block "data2")
  (doseq [block @blockchain]
    (println block)))
