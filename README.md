# Backend (Spring Boot)

This is the API server.

## Run on macOS (zsh) without `.env`

Set environment variables in your terminal, then run Gradle.

```sh
cd backend
chmod +x gradlew

# Required
export DATABASE_URL='jdbc:postgresql://<host>/<db>?sslmode=require'
export DATABASE_USERNAME='<user>'
export DATABASE_PASSWORD='<password>'
export JWT_SECRET='<at-least-32-chars>'

# Optional
export GOOGLE_CLIENT_ID='<optional>'
export GOOGLE_CLIENT_SECRET='<optional>'
export FACEBOOK_CLIENT_ID='<optional>'
export FACEBOOK_CLIENT_SECRET='<optional>'
export GOONGMAP_KEY='<optional>'
export MAIL_USERNAME='<optional>'
export MAIL_PASSWORD='<optional>'

./gradlew bootRun
```

Tip: if you already have a semicolon-delimited string like `KEY=...;KEY2=...`, you must convert `;` to spaces and run it with `export` or `env` (don’t paste secrets into git).

## Run in IntelliJ (no `.env`)

In your Run/Debug configuration, set the same variables in **Environment variables**, then run the application.
