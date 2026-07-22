# CBT-Pulse Grid Architecture

## Architectural style

CBT-Pulse Grid is designed as a modular monolith. The backend is deployed as one Spring Boot application, while Spring Modulith establishes explicit domain boundaries within that application. This approach keeps development, transactions, testing, and offline deployment straightforward without sacrificing the option to extract modules into independent services later if scale or organizational needs justify it.

Modules should collaborate through deliberately exposed APIs and application events. Their internal types remain private to their owning modules. Entities, repositories, controllers, and business workflows will be introduced inside these boundaries in later development stages.

## Application modules

- **Identity and Access Management (`identity`)** manages planned users, roles, authentication context, authorization policies, and account lifecycle.
- **Institution Management (`institution`)** manages institutional ownership, courses, organizational settings, and tenant-level context.
- **Question Bank (`questionbank`)** manages questions, answer options, course-aligned question collections, and future question authoring workflows.
- **Examination Management (`examination`)** manages exam definitions, scheduling, configuration, assignments, and publication lifecycle.
- **Exam Attempts (`attempt`)** manages candidate sessions, allocated questions, submitted answers, timing, and attempt lifecycle.
- **Live Exam Monitoring (`monitoring`)** manages proctoring signals, anti-cheat events, and live invigilator notifications.
- **Results and Grading (`result`)** manages scoring, grading outcomes, result calculation, and publication.
- **Audit Trail (`audit`)** records security-sensitive and operational actions for accountability and investigation.

## System boundaries and communication

The React application is a separate frontend and is not embedded in the Spring Boot backend. It communicates with the backend through REST APIs for normal operations such as administration, exam setup, question management, candidate activity, and results. WebSocket connections are reserved for time-sensitive live invigilator alerts and monitoring updates.

PostgreSQL is the system of record. Module ownership rules should be reflected in the data model even though the modules share one database in the modular-monolith deployment.

## Deployment and offline operation

Docker provides reproducible local and server deployments of the backend, frontend, and PostgreSQL services. Kubernetes is the target orchestration platform for deployments that require scheduling, health management, scaling, and resilient service operation.

The platform must also operate on an offline institutional LAN. During offline operation, browsers connect to services hosted within the local network, and all core examination, monitoring, and persistence functions remain independent of public internet connectivity. External integrations must therefore be optional and must not sit on the critical exam-delivery path.
