# Planno Tasks - Documentacao Tecnica Completa

## 1. Visao geral

Este projeto evoluiu de um backend Java existente para uma base fullstack de um sistema operacional para operacao freelancer.

Objetivo do produto:

- gestao de clientes
- gestao de projetos
- tarefas em formato Kanban com atribuicao de usuarios
- pagamentos avulsos
- assinaturas recorrentes
- dashboard financeiro
- documentos em Google Drive
- calendario integrado com Google Calendar por usuario
- base de conhecimento estilo workspace interno
- notificacoes por email

Estado atual:

- backend Spring Boot multi-tenant com JWT
- frontend Angular com dashboard e modulos principais
- Mercado Pago integrado no backend para checkout avulso, checkout recorrente e webhook
- Google Drive migrado para arquitetura OAuth do usuario, com acesso ao Drive do usuario conectado
- Google Calendar integrado por usuario para leitura de eventos, importacao para tarefa e sync de tarefas para agenda
- H2 removido por completo do projeto; baseline atual e Neon/PostgreSQL

## 2. Stack confirmada no codigo

Backend:

- Java 21 como baseline do projeto em `pom.xml`
- Spring Boot `4.0.5`
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Flyway
- PostgreSQL
- Lombok
- JWT com `java-jwt`
- Java Mail

Frontend:

- Angular `21`
- standalone components
- Angular Router
- HttpClient
- Signals para estado local/global
- CDK Drag and Drop no Kanban
- SCSS

Banco:

- PostgreSQL no Neon
- migrations versionadas em `src/main/resources/db/migration`

## 3. Estrutura do repositorio

- `src/main/java/com/planno/dash_api`
  - `config`: seguranca, Jackson, async
  - `controller`: entrada HTTP
  - `service`: regra de negocio e integracoes
  - `repository`: acesso a dados
  - `entity`: modelo JPA
  - `dto`: requests, responses e mappers
  - `infra`: JWT, filtro de seguranca, contexto de tenant, exceptions
- `src/main/resources`
  - `application.properties`
  - `db/migration`
  - `templates/email`
- `frontend`
  - `src/app/core`: modelos, stores, auth, interceptors e utilitarios
  - `src/app/pages`: dashboard, clients, projects, tasks, subscriptions, payments, documents, knowledge-base
  - `src/styles.scss`: tema global
- `docs`
  - esta documentacao

## 4. Arquitetura backend

Padrao predominante:

- controller recebe request e retorna response
- service concentra a regra de negocio
- repository executa consultas JPA
- mapper converte entity <-> DTO

O projeto foi mantido no padrao ja existente. Nao houve troca de framework nem mudanca arquitetural radical.

### 4.1 Multi-tenancy

A aplicacao e multi-tenant por empresa.

Fluxo:

1. usuario autentica
2. JWT carrega o `tenantId`
3. `SecurityFilter` valida o token
4. `TenantContext` armazena o `tenantId` da requisicao atual
5. services e repositories filtram dados por tenant

Consequencia pratica:

- clientes, projetos, tarefas, pagamentos, assinaturas, documentos e paginas de conhecimento ficam isolados por tenant

### 4.2 Autenticacao

Fluxo atual:

- `POST /api/auth/register`
- `POST /api/auth/login`
- token JWT retornado para o frontend
- interceptor Angular envia `Authorization: Bearer <token>`

Rotas abertas por configuracao:

- login
- registro
- criacao de usuario
- criacao de tenant
- webhook do Mercado Pago
- callback OAuth do Google Drive

## 5. Dominios e modulos implementados

### 5.1 Tenants e usuarios

- criacao de tenant
- criacao de usuario
- login JWT
- consulta do usuario logado
- listagem de usuarios do tenant

### 5.2 Clients

- criar cliente
- listar clientes ativos
- exclusao logica
- criacao automatica de pasta em `/Clients/{name}` no storage quando habilitado

### 5.3 Projects

- CRUD principal
- owner opcional
- vinculacao com cliente
- criacao automatica de pasta em `/Projects/{name}`
- notificacao por email na criacao
- protecao de exclusao quando existirem tarefas ou pagamentos vinculados

### 5.4 Tasks

Modelo de atribuicao implementado:

- um responsavel `ManyToOne`
- varios participantes `ManyToMany`
- criador da tarefa registrado

Funcionalidades:

- criar tarefa
- atualizar status
- atribuir e desatribuir usuarios
- listar todas
- listar tarefas do usuario logado
- filtrar por usuario
- Kanban no frontend com drag-and-drop
- notificacao por email ao atribuir e ao mudar status

### 5.5 Subscriptions

- criar assinatura
- listar
- atualizar status
- gerar checkout recorrente no Mercado Pago
- sincronizar status vindo do gateway
- notificar atualizacoes por email

### 5.6 Payments

- criar pagamento avulso
- listar
- atualizar status manual
- gerar checkout do Mercado Pago
- armazenar `externalReference`, `externalPaymentId`, `externalPreferenceId` e `checkoutUrl`
- webhook para sincronizacao
- reflexo no dashboard
- notificacao de recebimento por email

### 5.7 Dashboard

Resumo financeiro e operacional:

- total de clientes
- projetos ativos
- caixa liquido
- receita aprovada
- receita recorrente
- pendencias
- distribuicao de tarefas
- destaque por cliente e carga por responsavel

### 5.8 Documents

Estado atual:

- backend nao armazena arquivos localmente
- documentos sao persistidos no Google Drive
- metadata fica no banco

Metadata salva:

- `storageFileId`
- `storageFolderId`
- `storageFolderPath`
- `name`
- `mimeType`
- `fileSize`
- `webViewUrl`
- `relationType`
- `relationId`

Relacoes suportadas:

- client
- project
- finance
- knowledge base
- general

### 5.9 Knowledge base

- criar paginas
- editar paginas
- excluir paginas
- busca
- preparacao para anexar documentos

### 5.10 Email notifications

Arquitetura implementada:

- `NotificationService` dispara eventos no service layer
- `EmailService` envia emails async
- `EmailTemplateService` renderiza templates HTML

Eventos implementados:

- tarefa atribuida
- status da tarefa alterado
- projeto criado
- pagamento recebido
- assinatura atualizada

### 5.11 Calendar sync

Estado atual:

- conexao OAuth do Google Calendar por usuario
- leitura de eventos da agenda conectada dentro da tela `/calendar`
- importacao de evento externo para tarefa interna
- sincronizacao explicita de tarefa do Planno para o Google Calendar
- vinculo task-event mantido por usuario, nao por tenant

Decisao arquitetural importante:

- como cada usuario pode querer sincronizar a propria agenda pessoal, o mapeamento do evento foi modelado por `task + user`, e nao diretamente na tabela de tarefas

## 6. Integracao Mercado Pago

## 6.1 O que ja esta pronto

- criacao de preferencia para pagamento avulso via `POST /checkout/preferences`
- criacao de assinatura recorrente via `POST /preapproval`
- webhook backend em `POST /api/payments/webhooks/mercado-pago`
- sincronizacao de pagamentos e assinaturas a partir do `external_reference`
- reflexo de status em pagamentos e subscriptions locais

Implementacao principal:

- `MercadoPagoService`
- `PaymentService`
- `SubscriptionService`

`external_reference` padronizado:

- pagamento: `tenant:{tenantId}:payment:{paymentId}`
- assinatura: `tenant:{tenantId}:subscription:{subscriptionId}`

Isso permite reconciliar notificacoes do gateway com os registros locais.

## 6.2 Variaveis de ambiente

- `MERCADO_PAGO_ENABLED`
- `MERCADO_PAGO_ACCESS_TOKEN`
- `MERCADO_PAGO_API_BASE_URL`
- `MERCADO_PAGO_CURRENCY_ID`
- `MERCADO_PAGO_SUCCESS_URL`
- `MERCADO_PAGO_PENDING_URL`
- `MERCADO_PAGO_FAILURE_URL`
- `MERCADO_PAGO_SUBSCRIPTION_BACK_URL`
- `MERCADO_PAGO_NOTIFICATION_URL`

## 6.3 O que falta para producao

- subir o backend em HTTPS publico
- configurar `MERCADO_PAGO_NOTIFICATION_URL` com a URL publica real
- opcionalmente ativar credenciais de producao no painel
- adicionar validacao de assinatura do webhook como hardening adicional

## 6.4 Referencias oficiais

- Checkout Preferences: https://www.mercadopago.com.br/developers/pt/reference/preferences/_checkout_preferences/post
- Assinaturas sem plano associado: https://www.mercadopago.com.br/developers/pt/docs/subscriptions/integration-configuration/subscription-no-associated-plan/authorized-payments
- Notificacoes/Webhooks: https://www.mercadopago.com.br/developers/en/docs/checkout-pro/additional-content/notifications

## 7. Integracao Google Drive

## 7.1 Decisao arquitetural atual

A integracao foi ajustada para o modo que o negocio pediu: acesso ao Drive do proprio usuario, e nao service account isolada.

Arquitetura atual:

- OAuth 2.0 server-side
- escopo completo de Drive
- tokens persistidos por tenant
- callback backend
- frontend inicia a conexao e recebe o retorno apos consentimento

Entidades adicionadas:

- `GoogleDriveConnection`
- `GoogleDriveOAuthState`

Endpoints:

- `GET /api/integrations/google-drive/status`
- `POST /api/integrations/google-drive/connect-url`
- `DELETE /api/integrations/google-drive/connection`
- `GET /api/integrations/google-drive/callback`

Storage:

- `StorageService` continua como abstracao
- `GoogleDriveStorageService` e a implementacao ativa
- `DocumentService` consome apenas a interface

Isso preserva a arquitetura para futuras trocas de storage.

## 7.2 Fluxo OAuth implementado

1. frontend chama `POST /api/integrations/google-drive/connect-url`
2. backend gera `state` e URL de consentimento
3. usuario autentica no Google
4. Google redireciona para `GET /api/integrations/google-drive/callback`
5. backend troca `code` por `access_token` e `refresh_token`
6. backend salva conexao por tenant
7. frontend volta para a tela de documentos com status conectado

Com a conexao ativa:

- upload cria pastas automaticamente conforme a relacao
- download busca binario direto no Drive
- delete remove no Drive e no banco

## 7.3 Variaveis de ambiente

- `GOOGLE_DRIVE_ENABLED`
- `GOOGLE_DRIVE_CLIENT_ID`
- `GOOGLE_DRIVE_CLIENT_SECRET`
- `GOOGLE_DRIVE_REDIRECT_URI`
- `GOOGLE_DRIVE_AUTH_BASE_URL`
- `GOOGLE_DRIVE_TOKEN_URI`
- `GOOGLE_DRIVE_USERINFO_URI`
- `GOOGLE_DRIVE_REVOKE_URI`
- `GOOGLE_DRIVE_API_BASE_URL`
- `GOOGLE_DRIVE_SCOPES`
- `GOOGLE_DRIVE_ROOT_FOLDER_ID`
- `GOOGLE_DRIVE_FRONTEND_SUCCESS_URL`
- `GOOGLE_DRIVE_FRONTEND_ERROR_URL`

Observacao:

- `GOOGLE_DRIVE_ROOT_FOLDER_ID` pode ficar como `root` para operar na raiz do Drive conectado
- se quiser limitar a area da app dentro do Drive, basta informar o ID de uma pasta especifica

## 7.4 O que falta para ativar

- criar credencial OAuth Web Application no Google Cloud
- adicionar usuario de teste na tela de consentimento
- registrar o callback do backend
- fornecer `GOOGLE_DRIVE_CLIENT_ID` e `GOOGLE_DRIVE_CLIENT_SECRET`
- quando houver deploy, atualizar a redirect URI publica

## 7.5 Referencias oficiais

- OAuth web server flow: https://developers.google.com/identity/protocols/oauth2/web-server
- Drive scopes: https://developers.google.com/drive/api/guides/api-specific-auth

## 7.6 Google Calendar

Variaveis de ambiente:

- `GOOGLE_CALENDAR_ENABLED`
- `GOOGLE_CALENDAR_CLIENT_ID`
- `GOOGLE_CALENDAR_CLIENT_SECRET`
- `GOOGLE_CALENDAR_REDIRECT_URI`
- `GOOGLE_CALENDAR_AUTH_BASE_URL`
- `GOOGLE_CALENDAR_TOKEN_URI`
- `GOOGLE_CALENDAR_USERINFO_URI`
- `GOOGLE_CALENDAR_REVOKE_URI`
- `GOOGLE_CALENDAR_API_BASE_URL`
- `GOOGLE_CALENDAR_DEFAULT_CALENDAR_ID`
- `GOOGLE_CALENDAR_SCOPES`
- `GOOGLE_CALENDAR_FRONTEND_SUCCESS_URL`
- `GOOGLE_CALENDAR_FRONTEND_ERROR_URL`

Observacao:

- por padrao o backend aceita reutilizar o mesmo OAuth client do Google Drive se `GOOGLE_CALENDAR_CLIENT_ID` e `GOOGLE_CALENDAR_CLIENT_SECRET` nao forem informados separadamente
- o recorte inicial esta em `primary calendar`
- a sincronizacao atual e intencionalmente explicita e segura:
  - Google Calendar -> Planno: importar evento como tarefa
  - Planno -> Google Calendar: publicar/atualizar tarefa por acao do usuario

Proximo endurecimento recomendado:

- push notifications/webhook do Google Calendar para sync quase em tempo real
- escolha de calendario entre varios calendars do usuario

## 8. Frontend Angular

## 8.1 Estrutura

Rotas principais:

- `/dashboard`
- `/clients`
- `/projects`
- `/tasks`
- `/subscriptions`
- `/payments`
- `/documents`
- `/knowledge-base`

## 8.2 Caracteristicas implementadas

- layout dashboard moderno azul/branco
- dark mode
- login modal
- cadastro modal
- JWT interceptor
- stores com signals
- dashboard com cards e graficos simples
- Kanban com drag-and-drop
- pagamentos integrados a checkout URL
- documentos com status do Google Drive OAuth
- knowledge base com CRUD

## 8.3 Onde estao as partes principais

- rotas: `frontend/src/app/app.routes.ts`
- shell/app: `frontend/src/app/app.ts`
- auth store: `frontend/src/app/core/stores/auth.store.ts`
- documents store: `frontend/src/app/core/stores/documents.store.ts`
- tarefas: `frontend/src/app/pages/tasks`
- pagamentos: `frontend/src/app/pages/payments`
- documentos: `frontend/src/app/pages/documents`

## 9. Variaveis de ambiente backend

Configuracao principal em `src/main/resources/application.properties`.

### 9.1 Banco

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Exemplo Neon JDBC:

`jdbc:postgresql://HOST/neondb?sslmode=require&channelBinding=require`

### 9.2 JWT e CORS

- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`

### 9.3 Email

- `MAIL_ENABLED`
- `MAIL_FROM`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS`

### 9.4 Mercado Pago

- ver secao 6.2

### 9.5 Google Drive

- ver secao 7.3

## 10. Como rodar localmente

## 10.1 Backend

Requisitos:

- JDK 21 ou superior
- Maven Wrapper do projeto
- PostgreSQL/Neon configurado nas env vars

Passos:

1. definir um `JAVA_HOME` com JDK 21+
2. definir as env vars do banco e do JWT
3. executar `.\mvnw.cmd spring-boot:run`

Observacao importante:

- nesta maquina, o `java` padrao do PATH estava em 17
- a compilacao so foi validada ao forcar um JDK superior
- portanto, no ambiente do socio ou CI, garantir `JAVA_HOME` em JDK 21+

## 10.2 Frontend

Requisitos:

- Node.js atual
- dependencias instaladas em `frontend`

Passos:

1. entrar em `frontend`
2. executar `npm install`
3. executar `npm run start`

Build:

- `npm run build`

## 11. Deploy recomendado

Backend:

- container ou VM com JDK 21+
- variaveis de ambiente configuradas
- HTTPS publico

Frontend:

- build estatico do Angular
- servir via CDN, Vercel, Netlify, Nginx ou similar

Checklist minimo:

- Neon configurado
- JWT secret forte
- CORS apontando para o dominio do frontend
- Mercado Pago com URLs publicas
- Google OAuth com callback publico
- SMTP real se quiser notificacoes reais

## 12. Resultado da validacao tecnica

Validacoes executadas nesta rodada:

- smoke test funcional completo em base temporaria antes do retorno ao Neon
- retorno do projeto para Neon
- remocao total do H2 do codigo e dos artefatos locais
- compilacao backend validada com JDK superior a 21
- frontend ja havia sido buildado com sucesso
- backend rodando respondeu `401` em login invalido, confirmando cadeia de seguranca ativa

## 13. Pendencias reais para fechar 100%

- credenciais OAuth do Google Drive
- URL publica HTTPS do backend para webhook e callback em producao
- SMTP real, se quiser disparo de email real
- endurecimento de webhook do Mercado Pago com verificacao de assinatura
- eventualmente uma estrategia de testes automatizados alinhada com PostgreSQL real

## 14. Recomendacao para o socio desenvolvedor

Prioridade de continuidade:

1. configurar JDK 21+ no ambiente local e CI
2. subir backend em URL publica HTTPS
3. ligar Mercado Pago com webhook publico
4. criar OAuth Web Application no Google Cloud e conectar o Drive
5. ativar SMTP real
6. adicionar testes integrados com PostgreSQL compativel com producao

Resumo final:

- a base do produto esta pronta para evolucao como SaaS
- a arquitetura atual esta coerente com o projeto original
- as integracoes externas mais sensiveis ja estao desenhadas no codigo
- o que falta agora e configuracao operacional e endurecimento de producao, nao uma reescrita da base
