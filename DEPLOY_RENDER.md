# Deploy no Render

Este repositório já está preparado para subir no Render com:

- `planno-api`: backend Spring Boot em Docker
- `planno-web`: frontend Angular como Static Site

O blueprint está em [render.yaml](/D:/Projetos%20Planno/dash-api/render.yaml).

## Antes de criar os serviços

Você vai precisar de um banco PostgreSQL. Para manter tudo gratuito agora, a opção mais prática é usar o plano free do Neon e copiar:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Também defina o slug do tenant interno da sua equipe em:

- `PLATFORM_BILLING_ADMIN_TENANT_SLUG`

Esse slug é o tenant que pode ver as telas internas de billing e provisionamento.

## Como subir

1. Faça login no Render.
2. Clique em `New +` > `Blueprint`.
3. Conecte o repositório `plannocorp/planno-dash`.
4. O Render vai ler o [render.yaml](/D:/Projetos%20Planno/dash-api/render.yaml) e criar:
   - `planno-api`
   - `planno-web`
5. Preencha as variáveis obrigatórias do backend:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `PLATFORM_BILLING_ADMIN_TENANT_SLUG`
6. Finalize o deploy.

## O que já fica pronto

- O frontend recebe automaticamente a URL pública do backend.
- O backend aceita automaticamente o domínio público do frontend no CORS.
- O health check do backend responde em `/api/health`.
- O frontend faz rewrite para `/index.html`, então as rotas do Angular funcionam direto no Render.

## Mercado Pago e integrações

No blueprint atual, estas integrações sobem desativadas:

- `MERCADO_PAGO_ENABLED=false`
- `GOOGLE_DRIVE_ENABLED=false`
- `GOOGLE_CALENDAR_ENABLED=false`

Isso é intencional para você publicar agora só para a sua equipe, sem depender das credenciais finais.

Quando for abrir a venda, ative e configure pelo menos:

- `MERCADO_PAGO_ENABLED=true`
- `MERCADO_PAGO_ACCESS_TOKEN`
- `MERCADO_PAGO_WEBHOOK_SECRET`
- `MERCADO_PAGO_SUCCESS_URL`
- `MERCADO_PAGO_PENDING_URL`
- `MERCADO_PAGO_FAILURE_URL`
- `PLATFORM_BILLING_NOTIFICATION_URL`
- `PLATFORM_BILLING_MANAGE_URL`

## Observação sobre plano grátis

No plano grátis do Render, o backend pode entrar em sleep depois de um período sem uso. Para a sua equipe usando internamente agora, isso costuma ser aceitável, mas o primeiro acesso após idle pode demorar um pouco.
