# 🚀 Planno Task

> Plataforma fullstack para freelancers centralizarem tarefas, projetos, clientes, pagamentos, assinaturas, documentos e indicadores financeiros em um unico sistema.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-ready-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-111827?style=for-the-badge)
![API](https://img.shields.io/badge/API-REST-0A7C62?style=for-the-badge)

## ✨ Visao Geral

Planno Task foi desenhado para resolver um problema comum de freelancers e pequenos prestadores de servico: a gestao do negocio fica espalhada entre planilhas, Trello, WhatsApp, banco, Drive e plataformas de pagamento.

A proposta e transformar essa operacao em um produto SaaS simples e objetivo, onde o freelancer acompanha entregas, clientes, receitas, assinaturas, documentos e agenda em uma experiencia unica.

**Valor principal:** menos retrabalho operacional, mais visibilidade financeira e mais controle sobre a relacao com clientes.

## 🖼️ Demonstracao / Preview

> Substitua os caminhos abaixo por screenshots reais do projeto antes de publicar o repositorio.

![Dashboard do Planno Task](docs/screenshots/dashboard.png)

![Kanban de tarefas](docs/screenshots/tasks-kanban.png)

![Gestao de documentos](docs/screenshots/documents.png)

## ✅ Funcionalidades

- **Dashboard financeiro e operacional:** indicadores de receita, despesas, clientes, projetos e tarefas em uma tela executiva.
- **Kanban de tarefas:** acompanhamento visual de prioridades, responsaveis, participantes e prazos.
- **Gestao de projetos:** organizacao de escopo, cliente, responsavel, orcamento e janela de execucao.
- **CRM de clientes:** cadastro de contatos, documentos e historico financeiro por workspace.
- **Pagamentos e vendas avulsas:** registro de contas a pagar, receitas pontuais e movimentacoes financeiras.
- **Assinaturas recorrentes:** controle de planos e status de assinatura.
- **Integracao Mercado Pago:** base para checkout, cobrancas e webhooks de pagamento.
- **Integracao Google Drive:** organizacao de arquivos por cliente, projeto, financeiro, base de conhecimento ou geral.
- **Agenda com Google Calendar:** leitura e sincronizacao de tarefas com calendario externo.
- **Base de conhecimento:** documentacao interna de processos, decisoes e playbooks.
- **Notificacoes por e-mail:** comunicacoes para tarefas, projetos, pagamentos e assinaturas.
- **Autenticacao JWT:** login seguro, isolamento por workspace e protecao das rotas da API.

## 🧱 Stack Tecnologica

### Backend

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- Flyway
- Lombok
- Auth0 Java JWT
- Springdoc OpenAPI / Swagger UI

### Frontend

- Angular 21
- Standalone Components
- Angular Router
- HttpClient
- Signals
- SCSS
- Vitest

### Banco de Dados

- PostgreSQL para ambientes reais
- H2 persistente para desenvolvimento local e demonstracao
- Migrations versionadas com Flyway

### Integracoes

- Mercado Pago para pagamentos e assinaturas
- Google Drive para armazenamento e organizacao de documentos
- Google Calendar para agenda e sincronizacao de tarefas
- SMTP para envio de notificacoes por e-mail

## 🏗️ Arquitetura

O projeto segue uma arquitetura fullstack separada por responsabilidades:

```text
Frontend Angular
  |
  | HTTP + JWT
  v
Backend Spring Boot
  |
  | JPA / Flyway
  v
PostgreSQL / H2
  |
  +--> Mercado Pago
  +--> Google Drive
  +--> Google Calendar
  +--> SMTP
```

No backend, a estrutura e organizada por camadas:

- **Controllers:** entrada HTTP e contratos REST.
- **DTOs e Mappers:** separacao entre modelo de dominio e payloads publicos.
- **Services:** regras de negocio, validacoes e orquestracao das integracoes.
- **Repositories:** persistencia com Spring Data JPA.
- **Entities:** modelo relacional do dominio.
- **Infra:** JWT, filtros de seguranca, handlers de erro e contexto do workspace.

No frontend, a aplicacao usa componentes standalone e stores por dominio, mantendo cada area do produto isolada: tarefas, clientes, projetos, documentos, pagamentos, dashboard e autenticacao.

## 🔌 Exemplos de API

### Autenticacao

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "teste@plannotasks.local",
  "password": "planno123"
}
```

Resposta:

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "name": "Usuario de Teste",
    "email": "teste@plannotasks.local",
    "tenantName": "Workspace local"
  }
}
```

### Criar Cliente

```http
POST /api/clients
Authorization: Bearer jwt-token
Content-Type: application/json
```

```json
{
  "name": "Studio Aurora",
  "email": "contato@studioaurora.com",
  "phone": "+55 11 99999-9999",
  "document": "12.345.678/0001-90"
}
```

### Criar Tarefa

```http
POST /api/tasks
Authorization: Bearer jwt-token
Content-Type: application/json
```

```json
{
  "title": "Revisar contrato e publicar cronograma",
  "description": "Validar entregas, responsaveis e proximos marcos do projeto.",
  "status": "BACKLOG",
  "priority": "MEDIUM",
  "projectId": 1,
  "responsibleUserId": 1,
  "participantUserIds": [1],
  "dueDate": "2026-04-30"
}
```

### Health Check

```http
GET /api/health
```

```json
{
  "status": "UP"
}
```

Documentacao local da API:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## ▶️ Como Rodar o Projeto

### Pre-requisitos

- Java 21
- Node.js 20+
- npm
- PostgreSQL, caso queira rodar fora do perfil H2

### 1. Clonar o repositorio

```bash
git clone https://github.com/iltonferreira/planno-task.git
cd planno-task
```

### 2. Configurar variaveis de ambiente

Use o arquivo `.env.example` como referencia. Para desenvolvimento rapido, o perfil `h2` exige apenas um segredo JWT local.

Exemplo minimo:

```env
APP_SECURITY_PRODUCTION=false
PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:4200
OPENAPI_ENABLED=true

SPRING_PROFILES_ACTIVE=h2
JWT_SECRET=change-me-use-at-least-32-characters

MERCADO_PAGO_ENABLED=false
GOOGLE_DRIVE_ENABLED=false
GOOGLE_CALENDAR_ENABLED=false
MAIL_ENABLED=false
```

Para PostgreSQL:

```env
DB_URL=jdbc:postgresql://localhost:5432/dash_api
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

### 3. Rodar o backend

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="h2"
$env:JWT_SECRET="change-me-use-at-least-32-characters"
.\mvnw.cmd spring-boot:run
```

API local:

```text
http://localhost:8080
```

### 4. Rodar o frontend

```powershell
cd frontend
npm install
npm run start
```

Aplicacao local:

```text
http://localhost:4200
```

### 5. Usuario de demonstracao

No perfil `h2`, a aplicacao provisiona um usuario local:

```text
E-mail: teste@plannotasks.local
Senha: planno123
```

## 📁 Estrutura do Projeto

```text
.
|-- backend
|   |-- src/main/java/com/planno/dash_api
|   |   |-- config
|   |   |-- controller
|   |   |-- dto
|   |   |-- entity
|   |   |-- enums
|   |   |-- infra
|   |   |-- repository
|   |   `-- service
|   |-- src/main/resources
|   |   |-- db/migration
|   |   |-- templates/email
|   |   |-- application.properties
|   |   `-- application-h2.properties
|   |-- pom.xml
|   `-- mvnw.cmd
|-- frontend
|   |-- src/app
|   |-- public
|   |-- scripts
|   |-- angular.json
|   `-- package.json
|-- docs
|-- .env.example
|-- .gitignore
`-- README.md
```

## 🔗 Integracoes

### Mercado Pago

O Mercado Pago e usado como base para pagamentos avulsos e assinaturas recorrentes. A API possui configuracoes para:

- criar links de pagamento;
- receber webhooks;
- validar assinatura do webhook;
- atualizar status de pagamento;
- acompanhar assinaturas e receitas recorrentes.

Variaveis principais:

```env
MERCADO_PAGO_ENABLED=true
MERCADO_PAGO_ACCESS_TOKEN=...
MERCADO_PAGO_WEBHOOK_SECRET=...
MERCADO_PAGO_SUCCESS_URL=http://localhost:4200/payments?status=success
MERCADO_PAGO_FAILURE_URL=http://localhost:4200/payments?status=failure
```

### Google Drive

O Google Drive e usado para organizar documentos do workspace por contexto de negocio:

- clientes;
- projetos;
- financeiro;
- base de conhecimento;
- arquivos gerais.

Variaveis principais:

```env
GOOGLE_DRIVE_ENABLED=true
GOOGLE_DRIVE_CLIENT_ID=...
GOOGLE_DRIVE_CLIENT_SECRET=...
GOOGLE_DRIVE_REDIRECT_URI=http://localhost:8080/api/integrations/google-drive/callback
```

### Google Calendar

A integracao com Google Calendar permite conectar uma agenda, visualizar eventos e sincronizar tarefas com datas de entrega.

```env
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CLIENT_ID=...
GOOGLE_CALENDAR_CLIENT_SECRET=...
GOOGLE_CALENDAR_REDIRECT_URI=http://localhost:8080/api/integrations/google-calendar/callback
```

## 🔐 Seguranca

- Autenticacao baseada em JWT.
- Rotas protegidas com Spring Security.
- Isolamento por tenant/workspace.
- Segredo JWT obrigatorio e sem fallback fraco em producao.
- CORS configuravel por ambiente.
- OpenAPI pode ser desabilitado em producao com `OPENAPI_ENABLED=false`.
- Integracoes externas ficam desativadas por padrao.
- Webhooks do Mercado Pago possuem validacao de assinatura.
- Upload de documentos possui limite de tamanho e sanitizacao de nome.
- Handler global evita vazar detalhes internos em erros genericos.

> Observacao: o frontend usa `localStorage` para persistir sessao JWT, uma escolha comum em SPAs. Em producao, isso exige atencao extra com CSP, revisao contra XSS e evitar HTML dinamico inseguro.

## 🧭 Roadmap

- [ ] Pagina publica de checkout para clientes finais.
- [ ] Relatorios financeiros exportaveis em CSV/PDF.
- [ ] Automacoes de lembretes por e-mail.
- [ ] Integracao com notas fiscais.
- [ ] Permissoes granulares por papel no workspace.
- [ ] Auditoria de eventos importantes.
- [ ] Templates de projetos recorrentes.
- [ ] Testes end-to-end para fluxos criticos.

## 👤 Casos de Uso

- **Freelancer solo:** controla entregas, clientes, prazos e recebimentos em uma unica plataforma.
- **Consultor recorrente:** acompanha assinaturas, pagamentos mensais e documentos por cliente.
- **Pequena agencia:** distribui tarefas por responsavel e acompanha carga de trabalho.
- **Prestador B2B:** organiza contratos, propostas e arquivos de cada projeto no Drive.
- **Operacao em crescimento:** usa dashboard financeiro para entender receita, despesas e pendencias.

## 💡 Por Que Esse Projeto Importa

Planno Task nao e apenas uma API CRUD. Ele representa um problema real de negocio: freelancers geralmente perdem tempo alternando entre ferramentas desconectadas e deixam dinheiro, prazos e informacoes importantes espalhados.

Ao centralizar gestao operacional e financeira, o sistema ajuda a:

- reduzir perda de contexto;
- melhorar previsibilidade de receita;
- dar visibilidade sobre entregas em andamento;
- profissionalizar a relacao com clientes;
- criar uma base tecnica pronta para evoluir para um SaaS.

Para recrutadores, o projeto demonstra dominio de backend moderno com Spring Boot, seguranca, integracoes externas, arquitetura em camadas, frontend Angular e preocupacao com produto.

Para clientes, demonstra uma visao clara: transformar gestao freelancer em um fluxo simples, rastreavel e escalavel.

## 👨‍💻 Autor

Desenvolvido por **Ilton Ferreira**.

Projeto criado com foco em arquitetura fullstack, integracoes reais e apresentacao profissional para portfolio.

GitHub: [github.com/iltonferreira](https://github.com/iltonferreira)

## 📄 Licenca

Este projeto esta disponivel para fins de estudo e portfolio. Caso deseje usar comercialmente, revise a licenca do repositorio e as credenciais das integracoes externas antes de publicar em producao.

