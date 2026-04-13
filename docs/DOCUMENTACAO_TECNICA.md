# Documentação Técnica - Planno Task

## Visão Geral

Planno Task é uma plataforma fullstack para freelancers e pequenos prestadores de serviço centralizarem a gestão de tarefas, projetos, clientes, documentos, pagamentos, assinaturas e indicadores financeiros.

O produto foi estruturado como uma base SaaS, com separação clara entre frontend, backend, persistência e integrações externas. A aplicação combina um painel operacional com módulos de gestão financeira, organização documental e sincronização com serviços de terceiros.

## Stack Tecnológica

### Backend

- Java 21
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- H2 para execução local com perfil de desenvolvimento
- Lombok
- Auth0 Java JWT
- Springdoc OpenAPI
- Java Mail

### Frontend

- Angular 21
- Standalone Components
- Angular Router
- HttpClient
- Signals
- SCSS
- Vitest

### Integrações

- Mercado Pago para pagamentos e assinaturas
- Google Drive para armazenamento e organização de documentos
- Google Calendar para agenda e sincronização de tarefas
- SMTP para notificações transacionais por e-mail

## Arquitetura

O Planno Task segue uma arquitetura fullstack com frontend e backend independentes. O frontend Angular consome a API REST protegida por JWT, enquanto o backend concentra regras de negócio, persistência, autenticação e integração com serviços externos.

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

### Backend em Camadas

- **Controllers:** expõem os contratos HTTP da API.
- **DTOs e Mappers:** isolam os payloads públicos do modelo interno.
- **Services:** concentram regras de negócio e orquestram integrações.
- **Repositories:** encapsulam acesso a dados com Spring Data JPA.
- **Entities:** representam o modelo relacional do domínio.
- **Infra:** reúne autenticação, filtros, contexto de workspace e tratamento de exceções.
- **Config:** centraliza segurança, CORS, Jackson e OpenAPI.

### Multi-workspace

A aplicação foi modelada para operar com workspaces isolados. Cada usuário pertence a um workspace, e os dados operacionais são consultados sempre dentro desse contexto.

Esse modelo permite separar clientes, projetos, tarefas, documentos, pagamentos, assinaturas e páginas da base de conhecimento por operação, mantendo a estrutura preparada para evolução SaaS.

### Autenticação

O backend utiliza autenticação baseada em JWT. Após login, o token é enviado pelo frontend nas requisições autenticadas usando o header:

```http
Authorization: Bearer <token>
```

O filtro de segurança valida o token, resolve o usuário autenticado e estabelece o contexto do workspace para a requisição.

## Estrutura do Projeto

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
|   |   |-- core
|   |   |-- pages
|   |   `-- shared
|   |-- public
|   |-- scripts
|   |-- angular.json
|   `-- package.json
|-- docs
|-- .env.example
|-- .gitignore
`-- README.md
```

## Módulos Implementados

### Dashboard

Consolida indicadores financeiros e operacionais da conta:

- total de clientes;
- projetos ativos;
- saldo líquido;
- receita aprovada;
- receita recorrente;
- contas em aberto;
- distribuição de tarefas;
- carga por responsável.

### Clientes

Módulo para gestão de clientes do workspace:

- cadastro de clientes;
- listagem de clientes ativos;
- exclusão lógica;
- vínculo com projetos, pagamentos, assinaturas e documentos.

### Projetos

Módulo para organização da entrega:

- cadastro e atualização de projetos;
- vínculo com cliente;
- responsável opcional;
- controle de orçamento e período;
- criação de pasta documental associada;
- bloqueio de exclusão quando existem tarefas ou pagamentos vinculados.

### Tarefas

Fluxo operacional em formato Kanban:

- status por coluna;
- responsável principal;
- participantes;
- prioridade;
- prazo;
- ordenação visual;
- listagem geral e por usuário;
- notificações por e-mail em eventos relevantes;
- sincronização com Google Calendar.

### Pagamentos

Controle financeiro de entradas e saídas:

- criação de pagamentos;
- atualização de status;
- associação com cliente, projeto ou assinatura;
- integração com gateway;
- reflexo nos indicadores do dashboard;
- notificação de pagamento recebido.

### Assinaturas

Controle de recorrência comercial:

- cadastro de assinaturas;
- status operacional;
- associação com clientes;
- integração com faturamento da plataforma;
- notificações de atualização.

### Documentos

Organização de arquivos por contexto de negócio:

- clientes;
- projetos;
- financeiro;
- base de conhecimento;
- arquivos gerais.

O backend mantém os metadados dos arquivos e usa uma abstração de storage para integração com o Google Drive.

### Base de Conhecimento

Espaço para documentação interna do workspace:

- criação de páginas;
- edição;
- exclusão;
- busca;
- páginas fixadas;
- vínculo com documentos.

### Notificações por E-mail

A camada de notificações é acionada pelos services de domínio e utiliza templates HTML para mensagens transacionais.

Eventos contemplados:

- tarefa atribuída;
- status de tarefa alterado;
- projeto criado;
- pagamento recebido;
- assinatura atualizada.

## Integrações

### Mercado Pago

A integração com Mercado Pago concentra fluxos de pagamento e faturamento:

- criação de links de pagamento;
- acompanhamento de status;
- recebimento de webhooks;
- reconciliação por referência externa;
- atualização dos registros financeiros internos.

Referências externas seguem um padrão que associa o evento do gateway ao workspace e ao registro local.

### Google Drive

O Google Drive é usado como provedor de armazenamento documental. A aplicação organiza arquivos em pastas por contexto de negócio e mantém no banco apenas os metadados necessários para consulta, download e remoção.

Fluxo principal:

1. usuário inicia conexão pelo frontend;
2. backend gera a URL OAuth;
3. Google retorna para o callback da API;
4. backend registra a conexão do workspace;
5. documentos passam a ser gravados no Drive conectado.

### Google Calendar

A integração com Google Calendar permite conectar uma agenda por usuário. O módulo suporta leitura de eventos, importação de eventos como tarefas e publicação de tarefas na agenda.

Essa modelagem permite que cada usuário sincronize sua própria agenda, mantendo o vínculo entre tarefa, usuário e evento externo.

### SMTP

O envio de e-mails é encapsulado por uma interface de serviço, permitindo alternar a configuração do provedor SMTP por ambiente.

## Frontend Angular

O frontend é organizado em módulos de produto dentro de `frontend/src/app/pages` e uma camada compartilhada em `frontend/src/app/core`.

Principais áreas:

- `dashboard`;
- `clients`;
- `projects`;
- `tasks`;
- `payments`;
- `documents`;
- `calendar`;
- `knowledge-base`;
- `workspace-plan`;
- `workspace-admin`.

Componentes de autenticação, stores, interceptors, models e utilitários ficam centralizados na camada `core` e em componentes compartilhados.

## Configuração Local

### Backend

Requisitos:

- Java 21;
- Maven Wrapper do projeto;
- PostgreSQL ou perfil local H2.

Execução com H2:

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

Documentação local da API:

```text
http://localhost:8080/swagger-ui.html
```

### Frontend

Requisitos:

- Node.js 20+;
- npm.

Execução:

```powershell
cd frontend
npm install
npm run start
```

Aplicação local:

```text
http://localhost:4200
```

Build:

```powershell
npm run build
```

## Variáveis de Ambiente

As principais configurações estão documentadas no arquivo `.env.example`.

Categorias principais:

- banco de dados: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`;
- autenticação: `JWT_SECRET`, `JWT_ISSUER`, `JWT_EXPIRATION_HOURS`;
- CORS: `CORS_ALLOWED_ORIGINS`;
- OpenAPI: `OPENAPI_ENABLED`;
- Mercado Pago: `MERCADO_PAGO_*`;
- Google Drive: `GOOGLE_DRIVE_*`;
- Google Calendar: `GOOGLE_CALENDAR_*`;
- e-mail: `MAIL_*`;
- frontend: `PLANNO_API_BASE_URL`.

## Visão de Deploy

Em um ambiente publicado, o frontend pode ser distribuído como build estático Angular em Vercel, Netlify, CDN, Nginx ou serviço equivalente.

O backend pode ser executado em um ambiente Java 21 com variáveis de ambiente configuradas e banco PostgreSQL gerenciado. A comunicação entre frontend e backend é feita por HTTPS, com CORS limitado ao domínio público do frontend.

Fluxo recomendado de infraestrutura:

```text
Usuário
  |
  v
Frontend estático
  |
  v
API Spring Boot
  |
  v
PostgreSQL
  |
  +--> Mercado Pago
  +--> Google APIs
  +--> SMTP
```

## Decisões Técnicas

- **API em camadas:** facilita manutenção, testes e evolução do domínio.
- **DTOs separados de entidades:** reduz acoplamento entre contrato público e persistência.
- **Flyway:** mantém a evolução do banco versionada.
- **JWT:** simplifica autenticação stateless entre SPA e API.
- **Storage por abstração:** permite trocar ou evoluir o provedor de documentos sem reescrever o domínio.
- **Integrações encapsuladas em services:** mantém os controllers enxutos e o domínio mais previsível.
- **Frontend por domínios:** melhora a organização das telas e dos stores conforme o produto cresce.

## Conclusão

Planno Task apresenta uma base técnica consistente para um produto SaaS voltado à operação freelancer. O projeto combina gestão de tarefas, clientes, finanças, documentos e integrações em uma arquitetura clara, com backend Spring Boot, frontend Angular e módulos de negócio organizados para evolução contínua.
