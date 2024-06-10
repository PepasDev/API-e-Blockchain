(ns financeiro.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clj-http.client :as client]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [financeiro.db :as db]
            [financeiro.transacoes :as transacoes]
            [financeiro.blockchain :as blockchain]))

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string conteudo)})

(defn dados [prompt]
  (println prompt)
  (read-line))

(defn exibir-resposta [resposta]
  (println (json/parse-string (:body resposta) true)))

(defn parse-input []
  (try
    (str/trim (read-line))
    (catch Exception e nil)))

(defn realizar-transacao []
  (let [transacao {:valor (read-string (dados "Valor da transacao:"))
                   :tipo (dados "Tipo da transacao:")
                   :rotulo (dados "Rotulo para a transacao:")}]
    (exibir-resposta
     (client/post "http://localhost:3000/transacoes"
                  {:headers {"Content-Type" "application/json"}
                   :body (json/generate-string transacao)}))))

(defn registrar-na-blockchain []
  (do
    (println "Minerando blockchain, isto pode demorar um pouco.")
    (blockchain/add-block (json/generate-string (json/parse-string (:body (client/get "http://localhost:3000/transacoes")) true)))
    (println "Transacoes registradas na blockchain.")))

(defn ver-transacoes []
  (exibir-resposta (client/get "http://localhost:3000/transacoes")))

(defn ver-blockchain []
  (doseq [block @blockchain/blockchain]
    (println block)))

(defn user-menu []
  (println "Escolha sua operacao")
  (println "=> A: Registrar transacoes")
  (println "=> B: Ver transacoes")
  (println "=> C: Registrar transacao na blockchain")
  (println "=> D: Ver blockchain")
  (println "Digite a opcao: ")
  
  (def input (parse-input))
  
  (cond
    (= input "A") (realizar-transacao)
    (= input "B") (ver-transacoes)
    (= input "C") (registrar-na-blockchain)
    (= input "D") (ver-blockchain)
    :else (println "Opcao invalida, tente novamente."))

)

(defroutes app-routes
  
  (POST "/transacoes" requisicao
    (let [transacao (:body requisicao)]
      (if (transacoes/valida? transacao)
        (do
          
          ;; Adiciona a transação na blockchain
          ;; (blockchain/add-block (json/generate-string transacao))
          
          ;; Registra a transação no banco de dados.
          (db/registrar transacao)

          (como-json {:mensagem "Transacao registrada com sucesso"} 201)

        )
        (como-json {:mensagem "Requisicao invalida"} 422))))

  (GET "/transacoes" {filtros :params}
    (como-json {:transacoes (if (empty? filtros) (db/transacoes)
                                             (db/transacoes-com-filtro filtros))}))

  (route/not-found "Recurso nao encontrado"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))

(defn -main []
  (run-jetty app {:port 3000 :join? false})
  (doall (repeatedly user-menu))
)