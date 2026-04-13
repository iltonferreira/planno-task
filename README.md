# 🚀 Planno Task

> Plataforma fullstack para freelancers gerenciarem operação, clientes, entregas e finanças em um único workspace.

Planno Task centraliza o que normalmente fica espalhado entre planilhas, quadros Kanban, WhatsApp, Drive, agenda e plataformas de pagamento. O objetivo é dar ao freelancer uma visão clara do negócio: o que precisa ser entregue, quem está envolvido, quanto entrou, quanto está pendente e quais documentos sustentam cada projeto.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-ready-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-111827?style=for-the-badge)
![API](https://img.shields.io/badge/API-REST-0A7C62?style=for-the-badge)

## ✨ Visão Geral

Freelancers e pequenos prestadores de serviço costumam perder tempo alternando entre ferramentas desconectadas. Isso aumenta retrabalho, dificulta o acompanhamento financeiro e reduz previsibilidade sobre prazos, cobranças e relacionamento com clientes.

O Planno Task foi desenhado como uma base SaaS para organizar essa operação em um fluxo único:

- acompanhar tarefas e projetos em um Kanban operacional;
- manter clientes, documentos e histórico financeiro no mesmo contexto;
- controlar receitas, despesas, vendas avulsas e assinaturas;
- integrar pagamentos, agenda e arquivos externos;
- oferecer um dashboard para decisões rápidas.

**Valor principal:** transformar a gestão freelancer em um processo rastreável, financeiro e pronto para escala.

## 🎯 Problema vs Solução

| Problema real | Como o Planno Task resolve |
| --- | --- |
| Tarefas espalhadas entre Trello, WhatsApp e anotações | Kanban com prioridade, responsável, participantes e prazo |
| Receita e cobranças difíceis de acompanhar | Dashboard financeiro, pagamentos e assinaturas por workspace |
| Documentos perdidos em pastas soltas | Integração com Google Drive organizada por cliente, projeto e contexto |
| Agenda desconectada das entregas | Integração com Google Calendar para eventos e tarefas com data |
| Crescimento sem estrutura operacional | Arquitetura por camadas, multi-workspace e integrações desacopladas |

## 🖼️ Demonstração / Preview

> Adicione screenshots reais antes de divulgar o projeto em processos seletivos ou para clientes.

![Dashboard do Planno Task](docs/screenshots/dashboard.png)

![Kanban de tarefas](docs/screenshots/tasks-kanban.png)

![Gestão de documentos](docs/screenshots/documents.png)

## 📌 Status do Projeto

Projeto em desenvolvimento ativo, com foco em portfólio técnico e validação de produto SaaS.

- Backend Spring Boot com autenticação, domínio financeiro, integrações e documentação OpenAPI.
- Frontend Angular com páginas funcionais para operação, clientes, projetos, documentos, pagamentos e dashboard.
- Perfil local com H2 para demonstração rápida.
- Estrutura preparada para PostgreSQL em ambientes reais.

## ✅ Funcionalidades

- **Dashboard financeiro e operacional:** visão executiva de receita, despesas, clientes, projetos e tarefas.
- **Kanban de tarefas:** gestão visual de status, prioridade, responsáveis, participantes e prazos.
- **Gestão de projetos:** organização de escopo, cliente, responsável, orçamento e janela de execução.
- **CRM de clientes:** cadastro de contatos, documentos e histórico por workspace.
- **Pagamentos e vendas avulsas:** controle de entradas, saídas e movimentações financeiras.
- **Assinaturas recorrentes:** acompanhamento de planos, status e recorrência.
- **Integração Mercado Pago:** base para checkout, webhooks e atualização de pagamentos.
- **Integração Google Drive:** organização de arquivos por cliente, projeto, financeiro, base de conhecimento ou geral.
- **Integração Google Calendar:** conexão de agenda e sincronização de tarefas com datas importantes.
- **Base de conhecimento:** registro de processos, decisões e playbooks internos.
- **Notificações por e-mail:** comunicação transacional para tarefas, projetos, pagamentos e assinaturas.
- **Autenticação JWT:** rotas protegidas, isolamento por workspace e controle de sessão.

## 🧱 Stack Tecnológica

### Backend

- Java 21
- Spring Boot 4.0.5
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
- H2 persistente para desenvolvimento local e demonstração
- Migrations versionadas com Flyway

### Integrações

- Mercado Pago para pagamentos e assinaturas
- Google Drive para armazenamento e organização de documentos
- Google Calendar para agenda e sincronização de tarefas
- SMTP para envio de notificações por e-mail

## 🏗️ Arquitetura

O projeto separa frontend e backend em módulos independentes, mantendo responsabilidades claras entre interface, API, domínio, persistência e integrações externas.

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

No backend, a estrutura segue uma organização por camadas:

- **Controllers:** entrada HTTP, contratos REST e documentação da API.
- **DTOs e Mappers:** separação entre modelo de domínio e payloads públicos.
- **Services:** regras de negócio, validações e orquestração das integrações.
- **Repositories:** persistência com Spring Data JPA.
- **Entities:** modelo relacional do domínio.
- **Infra:** JWT, filtros de segurança, handlers de erro e contexto do workspace.

No frontend, a aplicação usa componentes standalone e áreas por domínio, mantendo tarefas, clientes, projetos, documentos, pagamentos, dashboard e autenticação com responsabilidades isoladas.

Essa abordagem demonstra decisões importantes para um produto real: baixo acoplamento, evolução incremental, integração com provedores externos e preparação para múltiplos workspaces.

## 🔌 Exemplos de API

### Autenticação

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
    "name": "Usuário de Teste",
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
  "description": "Validar entregas, responsáveis e próximos marcos do projeto.",
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

Documentação local da API:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## ▶️ Como Rodar o Projeto

### Pré-requisitos

- Java 21
- Node.js 20+
- npm
- PostgreSQL, caso queira rodar fora do perfil H2

### 1. Clonar o repositório

```bash
git clone https://github.com/iltonferreira/planno-task.git
cd planno-task
```

### 2. Configurar variáveis de ambiente

Use o arquivo `.env.example` como referência. Para desenvolvimento rápido, o perfil `h2` exige apenas um segredo JWT local.

Exemplo mínimo:

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
DB_URL=jdbc:postgresql://localhost:5432/planno_task
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

Aplicação local:

```text
http://localhost:4200
```

### 5. Usuário de demonstração

No perfil `h2`, a aplicação provisiona um usuário local:

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

## 🔗 Integrações

### Mercado Pago

O Mercado Pago é usado como base para pagamentos avulsos e assinaturas recorrentes. A API possui configurações para:

- criar links de pagamento;
- receber webhooks;
- validar assinatura do webhook;
- atualizar status de pagamento;
- acompanhar assinaturas e receitas recorrentes.

Variáveis principais:

```env
MERCADO_PAGO_ENABLED=true
MERCADO_PAGO_ACCESS_TOKEN=...
MERCADO_PAGO_WEBHOOK_SECRET=...
MERCADO_PAGO_SUCCESS_URL=http://localhost:4200/payments?status=success
MERCADO_PAGO_FAILURE_URL=http://localhost:4200/payments?status=failure
```

### Google Drive

O Google Drive é usado para organizar documentos do workspace por contexto de negócio:

- clientes;
- projetos;
- financeiro;
- base de conhecimento;
- arquivos gerais.

Variáveis principais:

```env
GOOGLE_DRIVE_ENABLED=true
GOOGLE_DRIVE_CLIENT_ID=...
GOOGLE_DRIVE_CLIENT_SECRET=...
GOOGLE_DRIVE_REDIRECT_URI=http://localhost:8080/api/integrations/google-drive/callback
```

### Google Calendar

A integração com Google Calendar permite conectar uma agenda, visualizar eventos e sincronizar tarefas com datas de entrega.

```env
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CLIENT_ID=...
GOOGLE_CALENDAR_CLIENT_SECRET=...
GOOGLE_CALENDAR_REDIRECT_URI=http://localhost:8080/api/integrations/google-calendar/callback
```

## 🔐 Segurança

- Autenticação baseada em JWT.
- Rotas protegidas com Spring Security.
- Isolamento por tenant/workspace.
- Segredo JWT obrigatório e sem fallback fraco em produção.
- CORS configurável por ambiente.
- OpenAPI pode ser desabilitado em produção com `OPENAPI_ENABLED=false`.
- Integrações externas ficam desativadas por padrão.
- Webhooks do Mercado Pago possuem validação de assinatura.
- Upload de documentos possui limite de tamanho e sanitização de nome.
- Handler global evita vazar detalhes internos em erros genéricos.

> Observação: o frontend usa `localStorage` para persistir sessão JWT, uma escolha comum em SPAs. Em produção, isso exige atenção extra com CSP, revisão contra XSS e evitar HTML dinâmico inseguro.

## 🧭 Roadmap

- [ ] Página pública de checkout para clientes finais.
- [ ] Relatórios financeiros exportáveis em CSV/PDF.
- [ ] Automações de lembretes por e-mail.
- [ ] Integração com notas fiscais.
- [ ] Permissões granulares por papel no workspace.
- [ ] Auditoria de eventos importantes.
- [ ] Templates de projetos recorrentes.
- [ ] Testes end-to-end para fluxos críticos.

## 👤 Casos de Uso

- **Freelancer solo:** controla entregas, clientes, prazos e recebimentos em uma única plataforma.
- **Consultor recorrente:** acompanha assinaturas, pagamentos mensais e documentos por cliente.
- **Pequena agência:** distribui tarefas por responsável e acompanha carga de trabalho.
- **Prestador B2B:** organiza contratos, propostas e arquivos de cada projeto no Drive.
- **Operação em crescimento:** usa dashboard financeiro para entender receita, despesas e contas em aberto.

## 💡 Por Que Esse Projeto Importa

Planno Task não é apenas um CRUD com tela bonita. Ele parte de um problema real: profissionais independentes precisam vender, entregar, cobrar, documentar e manter relacionamento com clientes sem ter a estrutura operacional de uma empresa maior.

Ao centralizar gestão operacional e financeira, o produto ajuda a:

- reduzir perda de contexto entre ferramentas;
- melhorar previsibilidade de receita;
- aumentar controle sobre entregas em andamento;
- profissionalizar a comunicação com clientes;
- criar uma base técnica pronta para evoluir para um SaaS.

Para recrutadores, o projeto demonstra maturidade em backend moderno com Spring Boot, segurança, integrações externas, arquitetura em camadas, frontend Angular e visão de produto.

Para clientes, mostra uma proposta clara: transformar a gestão freelancer em um fluxo simples, rastreável e escalável.

## 📣 Contato

Desenvolvido por **Ilton Ferreira**.

Projeto criado com foco em arquitetura fullstack, integrações reais e apresentação profissional para portfólio.

- GitHub: [github.com/iltonferreira](https://github.com/iltonferreira)
- Projeto: [github.com/iltonferreira/planno-task](https://github.com/iltonferreira/planno-task)

## 📄 Licença

Este projeto está disponível para fins de estudo e portfólio. Caso deseje usar comercialmente, revise a licença do repositório e as credenciais das integrações externas antes de publicar em produção.
