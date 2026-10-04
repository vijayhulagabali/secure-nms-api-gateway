# Secure API Gateway for NMS Northbound Interfaces

A secure API gateway prototype for protecting Network Management System (NMS) Northbound Interfaces (NBIs).

## Architecture

```text
External Client
      |
      | HTTPS
      v
+-------------------+
|   Kong Gateway    |
|-------------------|
| TLS Termination   |
| JWT Validation    |
| Rate Limiting     |
| Request Logging   |
+---------+---------+
          |
          v
+-------------------+
| Spring Boot NMS   |
| Northbound API    |
+---------+---------+
          |
          v
+-------------------+
|   PostgreSQL      |
+-------------------+

        ^
        |
+-------------------+
|     Keycloak      |
| OAuth2 / JWT      |
+-------------------+
Technologies
- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Kong API Gateway
- Keycloak
- OAuth 2.0 Client Credentials
- JWT / RS256
- HTTPS / TLS
- Docker
- Maven
Features
- Secure NMS Northbound REST API
- OAuth 2.0 authentication
- JWT validation at Kong
- RS256 token verification
- API rate limiting
- HTTPS/TLS
- Request logging through Kong
- PostgreSQL persistence
- External access restricted to the API Gateway
- Web dashboard for NMS device monitoring
API
Get all devices
GET /api/v1/devices

Requires:
Authorization: Bearer <JWT>

Get device
GET /api/v1/devices/{id}

Add device
POST /api/v1/devices
Content-Type: application/json

Example:
{
  "name": "Router-01",
  "ipAddress": "192.168.1.10",
  "type": "Router",
  "status": "UP"
}

Security
The NMS API is not intended to be accessed directly by external clients.
The intended flow is:
Client
  |
  | OAuth2 Client Credentials
  v
Keycloak
  |
  | JWT
  v
Kong Gateway
  |
  | Authorized request
  v
Spring Boot NMS API
  |
  v
PostgreSQL
Running the project
Start PostgreSQL
sudo systemctl start postgresql

Start Keycloak
sudo docker start keycloak

Start Kong
cd ~/kong
sudo docker compose up -d

Configure environment variables
export DB_URL="jdbc:postgresql://localhost:5432/nmsdb"
export DB_USERNAME="postgres"
export DB_PASSWORD="YOUR_DATABASE_PASSWORD"

export NMS_CLIENT_ID="nms-client"
export NMS_CLIENT_SECRET="YOUR_KEYCLOAK_CLIENT_SECRET"

Start Spring Boot
cd ~/Downloads/nms-nbi
./mvnw spring-boot:run

Dashboard
Open:
https://localhost:8443/

The local TLS certificate is self-signed for development/demo purposes.
Project Structure
src/
├── main/
│   ├── java/
│   │   └── com/nms/nbi/
│   │       ├── client/
│   │       ├── controller/
│   │       ├── model/
│   │       ├── repository/
│   │       └── service/
│   └── resources/
│       ├── static/
│       └── application.properties
│
├── .gitignore
├── pom.xml
└── README.md

Security Notes
Secrets, private TLS keys and local truststores are intentionally excluded from version control.
This project is a college/academic prototype and should be further hardened before production deployment.
