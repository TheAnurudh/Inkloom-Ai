# InkLoom Backend (Spring Boot 3 + JWT + MySQL)

## Run in Eclipse
1. Install JDK 17+ and MySQL 8. Make sure MySQL is running.
2. Eclipse: File -> Import -> Maven -> Existing Maven Projects -> select this folder.
3. Edit `src/main/resources/application.properties` (or set env vars `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`).
   The database `inkloom_db` is created automatically.
4. Right click `InkLoomApplication.java` -> Run As -> Java Application.
5. Watch the console: the demo runner prints the whole workflow and the blocked attacks.
6. Set `inkloom.demo.enabled=false` when you no longer want demo data on every start.

## Console tags
[HTTP] requests | [AUTH] login/register | [JWT] token | [ACCESS] permission decisions | [AI] analyzer
[CONTENT] [REVIEW] [ORG] [PRODUCT] workflow | [AUDIT] audit rows | [ERROR]/[VALIDATION]/[UNEXPECTED] errors | SQL lines

## Main endpoints (all except register/login/health need `Authorization: Bearer <token>`)
POST /api/auth/register | POST /api/auth/login | GET /api/auth/me
POST /api/organizations | GET /api/organizations/mine
POST /api/organizations/{id}/teams | POST|GET /api/organizations/{id}/members
PUT|GET /api/organizations/{id}/rules | POST /api/organizations/{id}/products
GET /api/organizations/{id}/content | GET /api/organizations/{id}/audit
GET /api/products?q= | GET /api/products/{id}
POST /api/content/analyze | POST /api/content | PUT /api/content/{id} | POST /api/content/{id}/submit
GET /api/content/mine | GET /api/content/{id}
GET /api/reviews/my-tasks | GET /api/reviews/tasks/{id} | POST /api/reviews/tasks/{id}/decision
GET /api/health

## Secrets (read before pushing to GitHub)
- Real passwords/keys go ONLY in `src/main/resources/application-local.properties` (git-ignored) or in environment variables.
- Copy `application-local.properties.example` -> `application-local.properties` and fill it in.
- Generate a JWT secret:  PowerShell: `$b=New-Object byte[] 48; [Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($b); [Convert]::ToBase64String($b)`
  or `openssl rand -base64 48`
- Before `git add .`, run `git status` and confirm application-local.properties is NOT listed.

## Email verification
Register -> API sends a link `<FRONTEND_URL>/verify-email?token=...` -> your frontend page calls
`POST /api/auth/verify-email {"token":"..."}` -> user can log in. Resend: `POST /api/auth/resend-verification {"email":"..."}`.
With `inkloom.mail.enabled=false` the link is printed in the console (`[MAIL][DEV ONLY]`).

## Real AI
`ai.enabled=true` + `ai.api-key` (or env `ANTHROPIC_API_KEY`). Reports show `source`: `AI+RULES`, `RULES` (AI off) or
`RULES_FALLBACK` (AI failed -> rule checker answered). The AI can only add flags, never remove rule flags.

## Deploy (Docker: Render / Railway / Fly.io ...)
Set these environment variables on the platform (never in Git):
SPRING_PROFILES_ACTIVE=prod, DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, CORS_ORIGINS, FRONTEND_URL,
MAIL_ENABLED=true, MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM, AI_ENABLED=true, ANTHROPIC_API_KEY.
The platform provides HTTPS and the PORT variable. Health check path: /api/health
