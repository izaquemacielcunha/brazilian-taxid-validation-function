🌐 **Languages**
- 🇧🇷 [Português](#️-brazilian-tax-id-validation-function)
- 🇺🇸 [English](#️-brazilian-tax-id-validation-function---en)
- 🇫🇷 [Français](#️-brazilian-tax-id-validation-function---fr)

# ⚡️ Brazilian Tax ID Validation Function

Esta é uma aplicação serveless desenvolvida em Java 21 utilizando o **Azure Functions**. O principal objetivo deste serviço é realizar a validação de números de CPF (Cadastro de Pessoas Físicas).

A função opera em duas etapas: primeiro, realiza uma validação local rigorosa do formato e dos dígitos verificadores. Em seguida, integra-se a um serviço externo [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/) para verificar o status e obter dados vinculados ao documento (como o nome do titular).

## 🛠️ Stack tecnológico

* **Java 21**
* **Azure Functions** (Core Tools v4)
* **Dagger 2** (Injeção de ependências)
* **Jackson** (Processamento de JSON)
* **SLF4J & Logback** (Monitoramento e logs)
* **JUnit 5 & Mockito** (Testes automatizados)

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter as seguintes ferramentas instaladas:

* [Java Developer Kit (JDK) 21](https://learn.microsoft.com/pt-br/azure/developer/java/fundamentals/java-support-on-azure)
  * O Azure utiliza o [OpenJDK](https://openjdk.org)
* [Apache Maven](https://maven.apache.org/) (versão 3.0 ou superior)
* [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
* [Azure Functions Core Tools](https://docs.microsoft.com/azure/azure-functions/functions-run-local) (versão 4.x)

## ⚙️ Configuração do projeto

As configurações de recursos da Azure estão centralizadas no arquivo `pom.xml`. Os recursos provisionados pelo Maven Plugin são:

* **Region:** `brazilsouth`
* **Resource Group:** `java-functions-group`
* **App Service Plan:** `java-functions-app-service-plan` (Tier: Consumption)
* **Storage Account:** `javafunctionstoreaccount`
* **OS Runtime:** Windows

### Variáveis de ambiente (local.settings.json)

Para rodar o projeto localmente, crie ou atualize o arquivo `local.settings.json` na raiz do projeto. Ele deve conter as credenciais de integração para a API externa. **Nunca realize o commit deste arquivo caso possua tokens reais de produção.**

O dados abaixo são da API de teste, eles são públicos e podem ser encontrados em [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/).

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "EXTERNAL_API_URL": "https://api.cpfcnpj.com.br/{token}/1/",
    "EXTERNAL_API_TOKEN": "5ae973d7a997af13f0aaf2bf60e65803"
  }
}
```

Além dessas variáveis de ambiente, você pode configurar a variável `ENV_LOG_LEVEL` para alterar dinâmicamente o nível de log (o valor default é `INFO`).

## 🚀 Executando localmente

1. Faça o clone do repositório
2. Navegue até o diretório raiz do projeto
3. Compile o código e construa o pacote com o Maven
```bash
mvn clean package
```
4. Inicie a Azure Function
```bash
mvn azure-functions:run
```
5. Você encontrará o endereço local da function no log do terminal

## 📖 Documentação da API (endpoints)

`GET /api/ValidateCpf?cpf=`

Valida o número de CPF informado, verificando seu formato, seus dígitos verificadores matemáticos e realizando uma consulta em base externa (`api.cpfcnpj.com.br`) para verificar sua regularidade e obter o nome do titular.

### 🔍 Parâmetros da requisição (query parameters)

| Parâmetro | Tipo   | Obrigatório | Descrição                             |
| --------- | ------ | ----------- | ------------------------------------- |
| `cpf`     | String | Sim         | (`75284466020`) ou (`752.844.660-20`) |

### 🟢 Exemplo de resposta - sucesso (HTTP 200 OK)

Quando o CPF possui formato válido e é encontrado de forma regular no provedor de dados externo.

```json
{
  "cpf": "035.793.960-30",
  "nome": "Keanu Reeves"
}
```

### 🔴 Exemplo de resposta - erro (HTTP 400 ou 500)

Quando há alguma inconsistência na validação ou falha na integração com o serviço externo, a API retorna um objeto de erro padronizado contendo um `errorCode` interno para facilitar o tratamento pelo lado do cliente.

```json
{
  "timestamp": "2026-02-25T08:48:31.54056",
  "errorCode": 100,
  "errorMessage": "Invalid CPF"
}
```

### 📊 Códigos de retorno (HTTP status)

| HTTP Status                 | Cenário                                                                                                                                 |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK`                    | A validação foi concluída com sucesso e o documento foi considerado regular                                                             |
| `400 Bad Request`           | Ocorreu um erro de validação negocial. Pode ser um erro de formato local ou o CPF foi considerado inválido/inexistente pela API externa |
| `500 Internal Server Error` | Erro interno no servidor. Causado por ausência de variáveis de ambiente, falhas de conexão com o serviço externo ou falhas inesperadas  |

### 📈 Tabela de códigos de erro

Abaixo estão os códigos de erro retornados na propriedade `errorCode` no corpo das respostas de falha (`400` ou `500`).

#### Erros de validação do CPF

| Código (`errorCode`) | Descrição (`errorMessage`)                  | Motivo                                                                                       |
| -------------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------- |
| **99**               | `CPF is required`                           | O parâmetro `cpf` não foi enviado ou está vazio                                              |
| **100**              | `Invalid CPF`                               | O CPF informado não passou na validação do dígito verificador ou foi retornado como inválido |
| **101**              | `CPF must have 11 digits`                   | A string do CPF enviada não possui exatos 11 dígitos numéricos                               |
| **102**              | `CPF not found in Federal Revenue database` | O documento não foi localizado na base de dados da Receita Federal                           |
| **150**              | `CPF cannot contain all identical digits`   | O CPF informado é composto por uma sequência repetida (ex: `111.111.111-11`)                 |

#### Erros de integração (provedor externo)

| Código (`errorCode`) | Descrição (`errorMessage`)                              | Motivo                                                               |
| -------------------- | ------------------------------------------------------- | -------------------------------------------------------------------- |
| **400**              | `Incorrect parameters`                                  | A chamada externa foi feita com parâmetros mal formatados            |
| **1000**             | `Invalid token. Token does not match the source IP`     | Problema de configuração ou restrição do Token de integração         |
| **1001**             | `Insufficient credits for the selected package`         | A conta vinculada ao Token ficou sem saldo para consultas            |
| **1002**             | `Account suspended or inactive. Please contact support` | A conta de serviço no provedor foi suspensa                          |
| **1003**             | `IP and token temporarily blacklisted`                  | O IP da Function foi bloqueado temporariamente pelo provedor externo |
| **1004**             | `Invalid or unavailable package ID`                     | O pacote configurado na URL não é mais válido                        |
| **1005**             | `CPF not allowed for this package`                      | O provedor bloqueou a consulta deste documento para o seu plano      |
| **1006**             | `Data provider is currently offline`                    | O serviço da base de dados ou do provedor está fora do ar no momento |
| **1007**             | `Rate limit exceeded. Maximum 20 requests per second`   | Muitas requisições simultâneas realizadas                            |

#### Erros internos da API

| Código (`errorCode`) | Descrição (`errorMessage`) | Motivo                                                                     |
| -------------------- | -------------------------- | -------------------------------------------------------------------------- |
| **500**              | `Internal server error`    | Faltam variáveis de ambiente ou ocorreu uma falha grave na execução        |
| **0**                | `Unmapped error code`      | O serviço externo retornou um código de falha ainda não mapeado no sistema |

## ☁️ Como fazer o deploy no Azure

O projeto já está configurado para ser implantado diretamente via Maven. Vá para a raiz do projeto e siga os passos abaixo:

1. Autenticação no Azure
```bash
az login
```
2. Executar o deploy
```bash
mvn clean package azure-functions:deploy
```
3. Configurar variáveis de ambiente
   1. No portal, no menu lateral da function, clique em Settings > Environment variables
   2. Adicione as duas variáveis:
      1. `EXTERNAL_API_URL`: `https://api.cpfcnpj.com.br/{token}/1/`
      2. `EXTERNAL_API_TOKEN`: `{token}`
   3. Clique em apply e confirme

### 🧪 Como testar

A function possui o nível de autorização `FUNCTION`, então será preciso passar a api key via header (`x-functions-key`) ou via query params (`code`). Você pode obter a "Function Key" no portal, em "App keys".

```bash
curl --location 'https://brazilian-taxid-validation-function.azurewebsites.net/api/ValidateCpf?cpf={value}' \
--header 'x-functions-key: {token}'
```
## 📋 TODO

* Melhorar a cobertura de testes
* Adicionar integração com algum banco de dados para salvar CPFs já consultados, afim de evitar os custos a cada consulta no serviço externo
* Adicionar integração com Azure Key Vault
* Adicionar algum mecanismo de resiliência

## 📚 Referências

A lógica utilizada para validar o CPF antes de consultar o serviço externo foi baseada no [artigo](https://clubes.obmep.org.br/blog/a-matematica-nos-documentos-a-matematica-dos-cpfs) do [Clubes de Matemática da OBMEP](https://clubes.obmep.org.br/blog).

O desenvolvimento da integração com [cpfcnpj](https://www.cpfcnpj.com.br) foi baseado na [documentação V1](https://www.cpfcnpj.com.br/dev).

# ⚡️ Brazilian Tax ID Validation Function - en

This is a serverless application developed in Java 21 using **Azure Functions**. The main goal of this service is to validate CPF (Cadastro de Pessoas Físicas - Brazilian Individual Taxpayer Registry) numbers.

The function operates in two stages: first, it performs a strict local validation of the format and check digits. Then, it integrates with an external service [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/) to check the document's status and retrieve linked data (such as the holder's name).

## 🛠️ Technology stack

* **Java 21**
* **Azure Functions** (Core Tools v4)
* **Dagger 2** (Dependency Injection)
* **Jackson** (JSON Processing)
* **SLF4J & Logback** (Monitoring and logs)
* **JUnit 5 & Mockito** (Automated tests)

## 📋 Prerequisites

Before starting, make sure you have the following tools installed:

* [Java Developer Kit (JDK) 21](https://learn.microsoft.com/en-us/azure/developer/java/fundamentals/java-support-on-azure)
  * Azure uses [OpenJDK](https://openjdk.org)
* [Apache Maven](https://maven.apache.org/) (version 3.0 or higher)
* [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
* [Azure Functions Core Tools](https://docs.microsoft.com/azure/azure-functions/functions-run-local) (version 4.x)

## ⚙️ Project configuration

Azure resource configurations are centralized in the `pom.xml` file. The resources provisioned by the Maven Plugin are:

* **Region:** `brazilsouth`
* **Resource Group:** `java-functions-group`
* **App Service Plan:** `java-functions-app-service-plan` (Tier: Consumption)
* **Storage Account:** `javafunctionstoreaccount`
* **OS Runtime:** Windows

### Environment variables (local.settings.json)

To run the project locally, create or update the `local.settings.json` file in the project's root directory. It must contain the integration credentials for the external API. **Never commit this file if it contains real production tokens.**

The data below is from the test API, it is public and can be found at [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/).

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "EXTERNAL_API_URL": "https://api.cpfcnpj.com.br/{token}/1/",
    "EXTERNAL_API_TOKEN": "5ae973d7a997af13f0aaf2bf60e65803"
  }
}
```

In addition to these environment variables, you can configure the `ENV_LOG_LEVEL` variable to dynamically change the log level (the default value is `INFO`).

## 🚀 Running locally

1. Clone the repository
2. Navigate to the project's root directory
3. Compile the code and build the package with Maven
```bash
mvn clean package
```
4. Start the Azure Function:
```bash
mvn azure-functions:run
```
5. You will find the local function address in the terminal log

## 📖 API documentation (endpoints)

`GET /api/ValidateCpf?cpf=`

Validates the provided CPF number, checking its format, its mathematical check digits, and performing a query on an external database (`api.cpfcnpj.com.br`) to verify its regularity and retrieve the holder's name.

### 🔍 Request parameters (query parameters)

| Parameter | Type   | Required | Description                           |
| --------- | ------ | -------- | ------------------------------------- |
| `cpf`     | String | Yes      | (`75284466020`) ou (`752.844.660-20`) |

### 🟢 Success response example (HTTP 200 OK)

When the CPF has a valid format and is found as regular by the external data provider.
```json
{
  "cpf": "035.793.960-30",
  "nome": "Keanu Reeves"
}
```

### 🔴 Error response example (HTTP 400 or 500)

When there is any inconsistency in validation or a failure in integration with the external service, the API returns a standardized error object containing an internal `errorCode` to facilitate error handling on the client side.
```json
{
  "timestamp": "2026-02-25T08:48:31.54056",
  "errorCode": 100,
  "errorMessage": "Invalid CPF"
}
```

### 📊 Return codes (HTTP status)

| HTTP Status                 | Scenario |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK`                    | Validation was completed successfully and the document is considered regular                                                                 |
| `400 Bad Request`           | A business validation error occurred. This could be a local format error, or the CPF was considered invalid/non-existent by the external API |
| `500 Internal Server Error` | Internal server error. Caused by missing environment variables, connection failures with the external service, or unexpected failures        |

### 📈 Error codes table

Below are the error codes returned in the `errorCode` property within the body of failure responses (`400` or `500`).

#### CPF validation errors

| Code (`errorCode`) | Description (`errorMessage`)                | Reason                                                                         |
| ------------------ | ------------------------------------------- | ------------------------------------------------------------------------------ |
| **99**             | `CPF is required`                           | The `cpf` parameter was not sent or is empty                                   |
| **100**            | `Invalid CPF`                               | The provided CPF failed the check digit validation or was returned as invalid  |
| **101**            | `CPF must have 11 digits`                   | The submitted CPF string does not have exactly 11 numeric digits               |
| **102**            | `CPF not found in Federal Revenue database` | The document was not located in the Federal Revenue (Receita Federal) database |
| **150**            | `CPF cannot contain all identical digits`   | The provided CPF consists of a repeating sequence (e.g., `111.111.111-11`)     |

#### Integration errors (external provider)

| Code (`errorCode`) | Description (`errorMessage`)                            | Reason                                                             |
| ------------------ | ------------------------------------------------------- | ------------------------------------------------------------------ |
| **400**            | `Incorrect parameters`                                  | The external call was made with poorly formatted parameters        |
| **1000**           | `Invalid token. Token does not match the source IP`     | Integration Token configuration issue or restriction               |
| **1001**           | `Insufficient credits for the selected package`         | The account linked to the Token has run out of query credits       |
| **1002**           | `Account suspended or inactive. Please contact support` | The service account at the provider has been suspended             |
| **1003**           | `IP and token temporarily blacklisted`                  | The Function's IP was temporarily blocked by the external provider |
| **1004**           | `Invalid or unavailable package ID`                     | The package configured in the URL is no longer valid               |
| **1005**           | `CPF not allowed for this package`                      | The provider blocked the query of this document for your plan      |
| **1006**           | `Data provider is currently offline`                    | The database or provider service is currently down                 |
| **1007**           | `Rate limit exceeded. Maximum 20 requests per second`   | Too many simultaneous requests were made                           |

#### Internal API errors

| Code (`errorCode`) | Description (`errorMessage`) | Reason                                                                    |
| ------------------ | ---------------------------- | ------------------------------------------------------------------------- |
| **500**            | `Internal server error`      | Environment variables are missing or a severe execution failure occurred  |
| **0**              | `Unmapped error code`        | The external service returned a failure code not yet mapped in the system |

## ☁️ How to deploy to Azure

The project is already configured to be deployed directly via Maven. Go to the root of the project and follow the steps below:

1. Azure Authentication
```bash
az login
```
2. Execute the deploy
```bash
mvn clean package azure-functions:deploy
```
3. Configure environment variables
   1. In the portal, on the function's side menu, click Settings > Environment variables
   2. Add the two variables
      1. `EXTERNAL_API_URL`: `https://api.cpfcnpj.com.br/{token}/1/`
      2. `EXTERNAL_API_TOKEN`: `{token}`
   3. Click Apply and confirm

### 🧪 How to test

The function has the `FUNCTION` authorization level, so you will need to pass the API key via header (`x-functions-key`) or via query params (`code`). You can obtain the "Function Key" in the Azure portal, under "App keys".

```bash
curl --location 'https://brazilian-taxid-validation-function.azurewebsites.net/api/ValidateCpf?cpf={value}' \
--header 'x-functions-key: {token}'
```

## 📋 TODO

* Improve test coverage
* Add integration with a database to save previously queried CPFs, to avoid costs for every query made to the external service
* Add integration with Azure Key Vault
* Add a resilience mechanism

## 📚 References

The logic used to validate the CPF before querying the external service was based on the [article](https://clubes.obmep.org.br/blog/a-matematica-nos-documentos-a-matematica-dos-cpfs) by the [OBMEP Math Clubs](https://clubes.obmep.org.br/blog).

The development of the integration with [cpfcnpj](https://www.cpfcnpj.com.br) was based on the [V1 documentation](https://www.cpfcnpj.com.br/dev).

# ⚡️ Brazilian Tax ID Validation Function - fr

Il s'agit d'une application serverless développée en Java 21 utilisant **Azure Functions**. L'objectif principal de ce service est d'effectuer la validation des numéros de CPF (Cadastro de Pessoas Físicas - Registre des Personnes Physiques brésilien).

La fonction opère en deux étapes : d'abord, elle effectue une validation locale rigoureuse du format et des chiffres de contrôle. Ensuite, elle s'intègre à un service externe [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/) pour vérifier le statut et obtenir les données liées au document (comme le nom du titulaire).

## 🛠️ Stack technologique

* **Java 21**
* **Azure Functions** (Core Tools v4)
* **Dagger 2** (Injection de dépendances)
* **Jackson** (Traitement de JSON)
* **SLF4J & Logback** (Surveillance et logs)
* **JUnit 5 & Mockito** (Tests automatisés)

## 📋 Prérequis

Avant de commencer, assurez-vous d'avoir installé les outils suivants :

* [Java Developer Kit (JDK) 21](https://learn.microsoft.com/pt-br/azure/developer/java/fundamentals/java-support-on-azure)
  * Azure utilise [OpenJDK](https://openjdk.org)
* [Apache Maven](https://maven.apache.org/) (version 3.0 ou supérieure)
* [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
* [Azure Functions Core Tools](https://docs.microsoft.com/azure/azure-functions/functions-run-local) (version 4.x)

## ⚙️ Configuration du projet

Les configurations de ressources Azure sont centralisées dans le fichier `pom.xml`. Les ressources provisionnées par le plugin Maven sont :

* **Région :** `brazilsouth`
* **Groupe de ressources (Resource Group) :** `java-functions-group`
* **Plan de service (App Service Plan) :** `java-functions-app-service-plan` (Tier : Consumption)
* **Compte de stockage (Storage Account) :** `javafunctionstoreaccount`
* **OS Runtime :** Windows

### Variables d'environnement (local.settings.json)

Pour exécuter le projet localement, créez ou mettez à jour le fichier `local.settings.json` à la racine du projet. Il doit contenir les informations d'identification d'intégration pour l'API externe. **Ne validez (commit) jamais ce fichier si vous possédez des jetons de production réels.**

Les données ci-dessous proviennent de l'API de test, elles sont publiques et peuvent être trouvées sur [cpfcnpj.com.br](https://www.cpfcnpj.com.br/dev/).

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "EXTERNAL_API_URL": "https://api.cpfcnpj.com.br/{token}/1/",
    "EXTERNAL_API_TOKEN": "5ae973d7a997af13f0aaf2bf60e65803"
  }
}
```

En plus de ces variables d'environnement, vous pouvez configurer la variable `ENV_LOG_LEVEL` pour modifier dynamiquement le niveau de journalisation (la valeur par défaut est `INFO`).

## 🚀 Exécution locale

1. Clonez le dépôt
2. Accédez au répertoire racine du projet
3. Compilez le code et construisez le package avec Maven
```bash
mvn clean package
```
4. Démarrez l'Azure Function
```bash
mvn azure-functions:run
```
5. Vous trouverez l'adresse locale de la fonction dans les journaux de votre terminal

## 📖 Documentation de l'API (endpoints)

`GET /api/ValidateCpf?cpf=`

Valide le numéro de CPF fourni, en vérifiant son format, ses chiffres de contrôle mathématiques et en effectuant une requête vers une base de données externe (`api.cpfcnpj.com.br`) pour vérifier sa régularité et obtenir le nom du titulaire.

### 🔍 Paramètres de la requête (query parameters)

| Paramètre | Type   | Obligatoire | Description                           |
| --------- | ------ | ----------- | ------------------------------------- |
| `cpf`     | String | Oui         | (`75284466020`) ou (`752.844.660-20`) |

### 🟢 Exemple de réponse - succès (HTTP 200 OK)

Lorsque le CPF possède un format valide et est trouvé de manière régulière chez le fournisseur de données externe.

```json
{
  "cpf": "035.793.960-30",
  "nome": "Keanu Reeves"
}
```

### 🔴 Exemple de réponse - erreur (HTTP 400 ou 500)

Lorsqu'il y a une incohérence dans la validation ou un échec d'intégration avec le service externe, l'API renvoie un objet d'erreur standardisé contenant un `errorCode` interne pour faciliter le traitement côté client.

```json
{
  "timestamp": "2026-02-25T08:48:31.54056",
  "errorCode": 100,
  "errorMessage": "Invalid CPF"
}
```

### 📊 Codes de retour (Statut HTTP)

| Statut HTTP                 | Scénario                                                                                                                                                          |
| --------------------------  | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK`                    | La validation s'est terminée avec succès et le document a été considéré comme régulier                                                                            |
| `400 Bad Request`           | Une erreur de validation métier s'est produite. Il peut s'agir d'une erreur de format local ou le CPF a été considéré comme invalide/inexistant par l'API externe |
| `500 Internal Server Error` | Erreur interne du serveur. Causée par l'absence de variables d'environnement, des échecs de connexion au service externe ou des pannes inattendues                |

### 📈 Tableau des codes d'erreur

Ci-dessous se trouvent les codes d'erreur renvoyés dans la propriété `errorCode` dans le corps des réponses d'échec (`400` ou `500`).

#### Erreurs de validation du CPF

| Code (`errorCode`) | Description (`errorMessage`)                | Motif                                                                                            |
| ------------------ | ------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| **99**             | `CPF is required`                           | Le paramètre `cpf` n'a pas été envoyé ou est vide                                                |
| **100**            | `Invalid CPF`                               | Le CPF fourni n'a pas passé la validation du chiffre de contrôle ou a été renvoyé comme invalide |
| **101**            | `CPF must have 11 digits`                   | La chaîne du CPF envoyée ne comporte pas exactement 11 chiffres numériques                       |
| **102**            | `CPF not found in Federal Revenue database` | Le document n'a pas été trouvé dans la base de données de la Recette Fédérale brésilienne        |
| **150**            | `CPF cannot contain all identical digits`   | Le CPF fourni est composé d'une séquence répétée (ex : `111.111.111-11`)                         |

#### Erreurs d'intégration (fournisseur externe)

| Code (`errorCode`) | Description (`errorMessage`)                            | Motif                                                                          |
| ------------------ | ------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **400**            | `Incorrect parameters`                                  | L'appel externe a été fait avec des paramètres mal formatés                    |
| **1000**           | `Invalid token. Token does not match the source IP`     | Problème de configuration ou restriction du jeton (Token) d'intégration        |
| **1001**           | `Insufficient credits for the selected package`         | Le compte lié au jeton n'a plus de solde pour effectuer des requêtes           |
| **1002**           | `Account suspended or inactive. Please contact support` | Le compte de service chez le fournisseur a été suspendu                        |
| **1003**           | `IP and token temporarily blacklisted`                  | L'IP de la Function a été temporairement bloquée par le fournisseur externe    |
| **1004**           | `Invalid or unavailable package ID`                     | Le forfait configuré dans l'URL n'est plus valide                              |
| **1005**           | `CPF not allowed for this package`                      | Le fournisseur a bloqué la consultation de ce document pour votre forfait      |
| **1006**           | `Data provider is currently offline`                    | Le service de la base de données ou du fournisseur est actuellement hors ligne |
| **1007**           | `Rate limit exceeded. Maximum 20 requests per second`   | Trop de requêtes simultanées ont été effectuées                                |

#### Erreurs internes de l'API

| Code (`errorCode`) | Description (`errorMessage`) | Motif                                                                                                     |
| ------------------ | ---------------------------- | --------------------------------------------------------------------------------------------------------- |
| **500**            | `Internal server error`      | Des variables d'environnement sont manquantes ou une défaillance grave s'est produite lors de l'exécution |
| **0**              | `Unmapped error code`        | Le service externe a renvoyé un code d'erreur qui n'est pas encore mappé dans le système                  |

## ☁️ Comment déployer sur Azure

Le projet est déjà configuré pour être déployé directement via Maven. Allez à la racine du projet et suivez les étapes ci-dessous :

1. Authentification sur Azure
```bash
az login
```
2. Exécuter le déploiement
```bash
mvn clean package azure-functions:deploy
```
3. Configurer les variables d'environnement
   1. Dans le portail, dans le menu latéral de la fonction, cliquez sur Settings > Environment variables
   2. Ajoutez les deux variables
      1. `EXTERNAL_API_URL`: `https://api.cpfcnpj.com.br/{token}/1/`
      2. `EXTERNAL_API_TOKEN`: `TOKEN`
   3. Cliquez sur apply et confirmez.

### 🧪 Comment tester

La fonction possède le niveau d'autorisation `FUNCTION`, il sera donc nécessaire de passer la clé d'API via le header (`x-functions-key`) ou via les paramètres de requête (`code`). Vous pouvez obtenir la "Function Key" dans le portail, sous "App keys".

```bash
curl --location 'https://brazilian-taxid-validation-function.azurewebsites.net/api/ValidateCpf?cpf={value}' \
--header 'x-functions-key: {token}'
```

## 📋 TODO (À FAIRE)

* Améliorer la couverture des tests
* Ajouter une intégration avec une base de données pour sauvegarder les CPF déjà consultés, afin d'éviter les coûts à chaque consultation du service externe
* Ajouter une intégration avec Azure Key Vault
* Ajouter un mécanisme de résilience

## 📚 Références

La logique utilisée pour valider le CPF avant de consulter le service externe a été basée sur l'[article](https://clubes.obmep.org.br/blog/a-matematica-nos-documentos-a-matematica-dos-cpfs) du [Clubes de Matemática da OBMEP](https://clubes.obmep.org.br/blog).

Le développement de l'intégration avec [cpfcnpj](https://www.cpfcnpj.com.br) a été basé sur la [documentation V1](https://www.cpfcnpj.com.br/dev).