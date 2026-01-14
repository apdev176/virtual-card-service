# Git Commit History - Virtual Card Service

This document shows the development progression of the Virtual Card Service project.

## Commit Timeline

```
* d2cef7f - clean up
* 231e4bb - read me
* 703a5ab - test class
* fd30c6d - docker file added
* 4a4f374 - code refactoring
* cdab8ca - additional exception handles
* 148b145 - controller and service impl
* 232f0db - global exception handler
* ee5ee61 - repository
* 9a9f6dd - dtos
* 487b599 - profiling dev,uat,prod
* 8cae4fa - added optimistic Locking in case of Concurrency
* 71bb2e8 - done configuration and added entity
* 2b88eb0 - dependency issue resolution
* 8889635 - project setup
* 266bb2f - Initial commit
```

## Development Phases

### Phase 1: Project Setup (Initial)
- **266bb2f** - Initial commit
- **8889635** - Project setup
- **2b88eb0** - Dependency issue resolution

### Phase 2: Core Entities & Configuration
- **71bb2e8** - Done configuration and added entity
- **8cae4fa** - Added optimistic locking (Concurrency control)
- **487b599** - Profiling dev/uat/prod

### Phase 3: API Layer Development
- **9a9f6dd** - DTOs (Request/Response objects)
- **ee5ee61** - Repository layer (Spring Data JPA)
- **232f0db** - Global exception handler
- **148b145** - Controller and service implementation

### Phase 4: Error Handling & Refinement
- **cdab8ca** - Additional exception handlers
- **4a4f374** - Code refactoring

### Phase 5: DevOps & Testing
- **fd30c6d** - Dockerfile added (Containerization)
- **703a5ab** - Test class (Unit & Concurrency tests)
- **231e4bb** - README documentation
- **d2cef7f** - Clean up

## Key Implementation Decisions (Visible in Commits)

1. **Optimistic Locking (8cae4fa):** Early decision to handle concurrency using `@Version` annotation
2. **Profile-Based Configuration (487b599):** Separate configs for dev/uat/prod environments
3. **Global Exception Handling (232f0db, cdab8ca):** Centralized error handling strategy
4. **Docker Support (fd30c6d):** Made application container-ready
5. **Comprehensive Testing (703a5ab):** Added unit tests and concurrency tests
