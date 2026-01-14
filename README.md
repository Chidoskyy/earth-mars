Earth vs Mars – Dockerized Voting Application

A multi-service, containerized voting application built to practice Docker, Docker Compose, and microservice architecture concepts using multiple technologies.

Users vote for their preferred home planet (Earth or Mars). Votes are processed asynchronously and persisted, and results are exposed via an API.

Browser
  ↓
Vote Service (Java / Spring Boot)
  ↓
Redis (In-memory Queue)
  ↓
Worker Service (.NET Background Worker)
  ↓
PostgreSQL (Persistent Database)
  ↓
Result Service (Node.js / Express API)

earth-mars-voting/
├── vote-java/          # Java Spring Boot voting UI
├── worker-dotnet/      # .NET background worker
├── result-node/        # Node.js results API
├── db/                 # (Optional) database scripts
├── docker-compose.yml
├── .env.example
└── README.md

Application Flow

User submits a vote via the Vote UI

Vote is pushed into Redis

Worker service consumes votes from Redis

Worker writes votes to PostgreSQL

Result service queries PostgreSQL and exposes aggregated results

Running the Application

docker compose up --build


ccessing the Services
Service	URL
Vote UI	http://localhost:8080

Results API	http://localhost:3000/results

Purpose

This project was built for learning and practice, not as a production-ready system.
It serves as a hands-on reference for Docker, microservices, and container orchestration concepts.

.env

node_modules
bin
obj
target

.DS_Store
