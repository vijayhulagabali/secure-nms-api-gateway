# Secure API Gateway for NMS Northbound Interfaces

A secure Northbound Interface (NBI) architecture for a Network Management System (NMS), using **Kong API Gateway**, **Keycloak OAuth2/OIDC**, **JWT authentication**, **HTTPS/TLS**, **rate limiting**, **Spring Boot**, and **PostgreSQL**.

## Architecture

```text
                    External Client
                    Postman / App
                         |
                         | HTTPS :8443
                         v
                +---------------------+
                |    Kong Gateway     |
                |---------------------|
                | TLS Termination      |
                | JWT Validation       |
                | Rate Limiting        |
                | Access Logging       |
                +----------+----------+
                           |
                           | Private Docker Network
                           v
                +---------------------+
                | Spring Boot NMS NBI |
                |      :8080          |
                +----------+----------+
                           |
                           v
                    +-------------+
                    | PostgreSQL   |
                    |    nmsdb     |
                    +-------------+

                    +-------------+
                    |  Keycloak   |
                    |    :8081    |
                    | OAuth2/OIDC |
                    +-------------+
Security Flow
1. The client requests an OAuth2 access token from Keycloak.
2. Keycloak issues a signed JWT.
3. The client sends the JWT to Kong over HTTPS.
4. Kong validates the JWT before forwarding the request.
5. Kong applies rate limiting.
6. Only authenticated and permitted requests reach the Spring Boot NMS API.
7. Spring Boot communicates with PostgreSQL.
8. The NMS application is not exposed directly on host port 8080.
Technology Stack
Component	Technology
API Gateway	Kong Gateway
Authentication	Keycloak OAuth2/OIDC
Token	JWT / RS256
Backend	Java 17 + Spring Boot
Database	PostgreSQL
API Testing	Postman / cURL
Containerization	Docker
Security	HTTPS/TLS, JWT, Rate Limiting
Build	Maven


API Endpoints
Get all devices
GET /api/v1/devices

Get a device
GET /api/v1/devices/{id}

Add a device
POST /api/v1/devices
Content-Type: application/json

Example:
{
  "name": "Switch-02",
  "ipAddress": "192.168.1.20",
  "type": "Switch",
  "status": "UP"
}

Gateway Endpoints
The external API is accessed through Kong:
https://localhost:8443/api/v1/devices

The Spring Boot service listens internally on:
http://nms-nbi:8080

Port 8080 is not published to the host.
Authentication
Keycloak realm:
nms-realm

OAuth2 grant:
Client Credentials

Token endpoint:
http://localhost:8081/realms/nms-realm/protocol/openid-connect/token

The client must send:
Authorization: Bearer <access-token>

to Kong.
Rate Limiting
The NMS device API is configured with a demonstration limit of:
10 requests per minute

Requests exceeding the configured limit receive:
HTTP 429 Too Many Requests

The limit can be adjusted for a production deployment based on NMS capacity and expected traffic.
HTTPS
Kong terminates HTTPS on:
8443

The current project uses a self-signed certificate for the academic/lab environment.
For production, the certificate should be issued by a trusted Certificate Authority and use an appropriate DNS name/SAN configuration.
Dashboard
A simple NMS dashboard is available through Kong:
https://localhost:8443/

It displays:
- API Gateway status
- HTTPS/TLS status
- Authentication status
- Rate-limit information
- NMS device information
- Security controls
Project Structure
nms-nbi/
├── src/
│   ├── main/
│   │   ├── java/com/nms/nbi/
│   │   │   ├── client/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── static/
│   │       └── application.properties
│   └── test/
├── Dockerfile
├── .gitignore
├── pom.xml
└── README.md

Running the Backend
Build the Spring Boot application:
./mvnw clean package -DskipTests

Build the Docker image:
sudo docker build -t nms-nbi:latest .

The application requires the following environment variables:
DB_URL
DB_USERNAME
DB_PASSWORD
NMS_CLIENT_ID
NMS_CLIENT_SECRET

Secrets should be supplied through environment variables or a secure secrets-management system and should not be committed to Git.
Testing
Without JWT
curl -k -i https://localhost:8443/api/v1/devices

Expected:
401 Unauthorized

With a valid JWT
curl -k -i \
  https://localhost:8443/api/v1/devices \
  -H "Authorization: Bearer <ACCESS_TOKEN>"

Expected:
200 OK

Rate limiting
After exceeding the configured request limit:
429 Too Many Requests

Direct backend access
The backend should not be directly reachable through:
http://localhost:8080

External clients should use Kong instead.
Security Controls Demonstrated
- OAuth2 Client Credentials
- JWT-based authentication
- RS256 signature validation
- HTTPS/TLS
- API Gateway enforcement
- Rate limiting
- Private backend networking
- Access logging
- Environment-based secrets
- PostgreSQL persistence
- Docker container isolation
Important Note
This project is an academic/security prototype. The production version should additionally consider:
- Trusted CA certificates
- OIDC Authorization Code flow for interactive users
- Centralized secret management
- Key rotation
- Persistent Keycloak storage
- Kong database/HA deployment where appropriate
- Centralized monitoring and SIEM integration
- Network segmentation
- Production-grade firewall rules
- Backup and disaster recovery
License
This project is intended for educational and demonstration purposes.
