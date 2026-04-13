# Planno Tasks

Planno Tasks e uma plataforma fullstack multi-tenant para gestao operacional de clientes, projetos, tarefas, documentos, pagamentos, assinaturas e base de conhecimento. O produto foi preparado para venda com backend Spring Boot, frontend Angular e integracoes opcionais com Mercado Pago, Google Drive, Google Calendar e SMTP.

## Principais recursos

- Autenticacao JWT com isolamento por tenant/workspace.
- Dashboard operacional e financeiro.
- Gestao de clientes, projetos e tarefas em Kanban.
- Agenda de tarefas com integracao opcional ao Google Calendar.
- Upload, download e organizacao de documentos via Google Drive.
- Base de conhecimento interna por workspace.
- Pagamentos avulsos e assinaturas recorrentes via Mercado Pago.
- Painel interno para provisionar workspaces pagos ou cortesia.
- Notificacoes por email para tarefas, projetos, pagamentos e assinaturas.

## Stack

Backend:

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Lombok
- Auth0 Java JWT

Frontend:

- Angular 21
- Standalone components
- Angular Router
- HttpClient
- Signals
- SCSS
- Vitest

Infra:

- Dockerfile para o backend
- Blueprint Render em `render.yaml`
- Frontend estatico servido por build Angular
- Banco PostgreSQL externo, como Neon ou Render PostgreSQL

## Estrutura do projeto

```text
.
|-- src/main/java/com/planno/dash_api
|   |-- config          # Spring Security, CORS e Jackson
|   |-- controller      # APIs HTTP
|   |-- dto             # requests, responses e mappers
|   |-- entity          # entidades JPA
|   |-- enums           # enums de dominio
|   |-- infra           # JWT, filtro de seguranca, tenant context e exceptions
|   |-- repository      # repositories Spring Data
|   `-- service         # regras de negocio e integracoes
|-- src/main/resources
|   |-- application.properties
|   |-- db/migration    # migrations Flyway
|   `-- templates/email # templates HTML de email
|-- frontend            # aplicacao Angular
|-- docs                # documentacao tecnica complementar
|-- Dockerfile
|-- render.yaml
`-- DEPLOY_RENDER.md
```

## Seguranca aplicada

Esta versao recebeu endurecimentos importantes para producao:

- `JWT_SECRET` sem fallback fraco em producao.
- Validacao de tamanho minimo do segredo JWT.
- Issuer JWT configuravel por ambiente.
- Expiracao JWT configuravel.
- Parser de `Authorization: Bearer` mais estrito.
- Resposta `401` controlada para token invalido, expirado ou usuario removido.
- Limpeza explicita do `SecurityContext` e do `TenantContext` ao final da request.
- CORS bloqueia `*` quando credenciais estao habilitadas.
- Headers HTTP defensivos no backend: CSP restritiva para API, `X-Frame-Options`, HSTS e referrer policy.
- Handler global nao vaza mensagem interna de `RuntimeException`.
- Webhook Mercado Pago valida assinatura com comparacao constante e limite de idade para reduzir replay.
- Upload de documentos tem limite configuravel e sanitizacao de nome de arquivo.
- Download usa `Content-Disposition` seguro com filename codificado.

Importante: o frontend ainda guarda o JWT em `localStorage` para preservar sessao apos refresh. Isso e comum em SPAs simples, mas exige CSP forte no host do frontend, revisao constante contra XSS e evitar qualquer uso de HTML dinamico inseguro.

## Variaveis de ambiente

Obrigatorias em producao:

```env
APP_SECURITY_PRODUCTION=true
DB_URL=jdbc:postgresql://HOST:5432/DB?sslmode=require
DB_USERNAME=usuario
DB_PASSWORD=senha
JWT_SECRET=gere-um-segredo-com-32-caracteres-ou-mais
CORS_ALLOWED_ORIGINS=https://seu-frontend.com
PLATFORM_BILLING_ADMIN_TENANT_SLUG=slug-do-tenant-interno
```

Recomendadas:

```env
JWT_ISSUER=planno-tasks-api
JWT_EXPIRATION_HOURS=2
MAX_UPLOAD_SIZE_BYTES=10485760
JPA_SHOW_SQL=false
```

Mercado Pago:

```env
MERCADO_PAGO_ENABLED=true
MERCADO_PAGO_ACCESS_TOKEN=...
MERCADO_PAGO_WEBHOOK_SECRET=...
MERCADO_PAGO_SUCCESS_URL=https://seu-frontend.com/payments?status=success
MERCADO_PAGO_PENDING_URL=https://seu-frontend.com/payments?status=pending
MERCADO_PAGO_FAILURE_URL=https://seu-frontend.com/payments?status=failure
MERCADO_PAGO_SUBSCRIPTION_BACK_URL=https://seu-frontend.com/subscriptions
PLATFORM_BILLING_NOTIFICATION_URL=https://sua-api.com/api/platform-billing/webhooks/mercado-pago
PLATFORM_BILLING_MANAGE_URL=https://seu-frontend.com/workspace-plan
```

Google Drive:

```env
GOOGLE_DRIVE_ENABLED=true
GOOGLE_DRIVE_CLIENT_ID=...
GOOGLE_DRIVE_CLIENT_SECRET=...
GOOGLE_DRIVE_REDIRECT_URI=https://sua-api.com/api/integrations/google-drive/callback
GOOGLE_DRIVE_ROOT_FOLDER_ID=root
GOOGLE_DRIVE_FRONTEND_SUCCESS_URL=https://seu-frontend.com/documents?googleDrive=connected
GOOGLE_DRIVE_FRONTEND_ERROR_URL=https://seu-frontend.com/documents?googleDrive=error
```

Google Calendar:

```env
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CLIENT_ID=...
GOOGLE_CALENDAR_CLIENT_SECRET=...
GOOGLE_CALENDAR_REDIRECT_URI=https://sua-api.com/api/integrations/google-calendar/callback
GOOGLE_CALENDAR_FRONTEND_SUCCESS_URL=https://seu-frontend.com/calendar?googleCalendar=connected
GOOGLE_CALENDAR_FRONTEND_ERROR_URL=https://seu-frontend.com/calendar?googleCalendar=error
```

Email:

```env
MAIL_ENABLED=true
MAIL_FROM=no-reply@seudominio.com
MAIL_HOST=smtp.seudominio.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

## Rodando localmente

Backend:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/dash_api"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="<senha-local>"
$env:JWT_SECRET="<segredo-local-com-32-caracteres-ou-mais>"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
.\mvnw.cmd spring-boot:run
```

Backend com H2 persistente para teste local:

```powershell
$env:SPRING_PROFILES_ACTIVE="h2"
$env:JWT_SECRET="<segredo-local-com-32-caracteres-ou-mais>"
.\mvnw.cmd spring-boot:run
```

O perfil `h2` grava o banco em `data/planno-tasks.mv.db`, cria as tabelas com Hibernate e provisiona um usuario de teste:

```text
E-mail: teste@plannotasks.local
Senha: planno123
```

Frontend:

```powershell
cd frontend
npm install
npm run start
```

A API sobe por padrao em `http://localhost:8080` e o Angular em `http://localhost:4200`.

## Build e validacao

Backend:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Frontend:

```powershell
cd frontend
npm run build
npm test
```

## Deploy no Render

O arquivo `render.yaml` define:

- `planno-api`: backend Docker
- `planno-web`: frontend Angular estatico

Antes de ativar venda, confira no painel do Render:

- `APP_SECURITY_PRODUCTION=true`
- `JWT_SECRET` forte e gerado fora do repositorio
- `CORS_ALLOWED_ORIGINS` com o dominio real do frontend
- banco PostgreSQL com SSL
- webhooks Mercado Pago apontando para HTTPS publico
- redirects OAuth do Google cadastrados exatamente como as URLs publicas

Mais detalhes estao em `DEPLOY_RENDER.md`.

## Checklist antes de vender

- Rodar build/testes backend e frontend.
- Usar dominio HTTPS real para API e frontend.
- Configurar `APP_SECURITY_PRODUCTION=true`.
- Rotacionar `JWT_SECRET` se ele ja foi usado em ambiente inseguro.
- Configurar webhook secret do Mercado Pago.
- Testar pagamento avulso e assinatura recorrente em ambiente controlado.
- Revisar politicas de privacidade e termos, pois o sistema lida com dados de clientes, pagamentos e documentos.
- Garantir backup do PostgreSQL.
- Configurar logs e alertas para falhas de login, webhooks e integracoes externas.

## Licenca e uso comercial

Este repositorio representa a base comercial do Planno Tasks. Antes de criar uma copia publica para portfolio, remova credenciais, dados de clientes, URLs privadas e qualquer material visual ou texto que voce nao queira associar ao produto vendido.
