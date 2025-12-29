# Repository Authorization Implementation

## Overview

This implementation adds JWT-based authorization to ensure only repository owners can interact with their repositories, similar to GitHub's permission model.

## Architecture

### JWT Authentication
- Uses Keycloak as the identity provider
- JWT tokens contain a `sub` (subject) claim which is the user ID (UUID)
- All API endpoints under `/api/**` require authentication (configured in `SecurityConfiguration`)

### Owner-based Authorization
- Repository identifiers follow GitHub format: `owner/repository` (e.g., `95b54e9e-bbe3-4675-9c12-790ccf41dbb3/my-repo`)
- The `owner` part of the identifier matches the JWT's `sub` claim
- Only the owner can modify their repositories

## Key Components

### 1. Database Changes

**RepositoryEntity** - Added `ownerId` field:
```java
@Column(nullable = false)
private String ownerId;
```

**Repository DTO** - Added `ownerId` field for API responses:
```java
private String ownerId;
```

### 2. Security Components

**JwtHelper** (`/shared/security/JwtHelper.java`)
- Extracts user information from JWT tokens
- `getCurrentUserId()` - Returns the `sub` claim (user ID)
- `getClaim(String)` - Gets any claim from the JWT
- `getPreferredUsername()` - Returns the username
- `getEmail()` - Returns the email

**RepositoryAuthorizationService** (`/repository/internal/services/RepositoryAuthorizationService.java`)
- `extractOwnerFromIdentifier(String identifier)` - Extracts owner from `owner/repo` format
- `validateOwnerMatchesCurrentUser(String identifier)` - Ensures user can only create repos under their own ID
- `requireOwnership(UUID repositoryId)` - Validates user owns the repository (throws exception if not)
- `requireOwnershipByIdentifier(String identifier)` - Same but uses identifier instead of UUID

### 3. Exception Handling

**Custom Exceptions:**
- `RepositoryAccessDeniedException` - Thrown when user tries to access/modify repo they don't own
- `RepositoryNotFoundException` - Thrown when repository is not found

**GlobalExceptionHandler** (`/shared/exceptions/GlobalExceptionHandler.java`)
- Maps `RepositoryAccessDeniedException` → HTTP 403 Forbidden
- Maps `RepositoryNotFoundException` → HTTP 404 Not Found
- Maps `IllegalArgumentException` → HTTP 400 Bad Request
- Maps `IllegalStateException` → HTTP 409 Conflict

### 4. Service Changes

**RepositoryServiceImpl** - Updated methods:
- `createRepository()` - Validates owner in identifier matches current user, sets `ownerId`
- `cloneRepository()` - Validates owner in identifier matches current user, sets `ownerId`

**RepositoryFileServiceImpl** - Added authorization checks:
- `addFile()` - Requires ownership before adding files
- `updateFile()` - Requires ownership before updating files
- `deleteFile()` - Requires ownership before deleting files
- `createFolder()` - Requires ownership before creating folders
- `deleteFolder()` - Requires ownership before deleting folders

**Read operations (browse, list, read) are NOT restricted** - Anyone can view repositories.

## Authorization Flow

### Creating a Repository

1. User sends JWT token with request: `POST /api/repositories`
   ```json
   {
     "identifier": "95b54e9e-bbe3-4675-9c12-790ccf41dbb3/my-repo",
     "description": "My repository"
   }
   ```

2. `JwtHelper.getCurrentUserId()` extracts `sub` from JWT → `"95b54e9e-bbe3-4675-9c12-790ccf41dbb3"`

3. `RepositoryAuthorizationService.validateOwnerMatchesCurrentUser()` checks:
   - Extracts owner from identifier → `"95b54e9e-bbe3-4675-9c12-790ccf41dbb3"`
   - Compares with JWT's `sub`
   - If mismatch → throws `RepositoryAccessDeniedException` (403 Forbidden)

4. Repository is created with `ownerId` set to the user's `sub`

### Modifying a Repository

1. User sends request: `POST /api/repositories/owner/repo/files`

2. Controller resolves repository ID from identifier

3. `RepositoryAuthorizationService.requireOwnership(repositoryId)`:
   - Gets current user ID from JWT
   - Loads repository from database
   - Compares `repository.ownerId` with current user ID
   - If mismatch → throws `RepositoryAccessDeniedException` (403 Forbidden)

4. If ownership validated, operation proceeds

## Security Guarantees

✅ **User can only create repositories under their own user ID**
- Prevents impersonation: User A cannot create repos as "UserB/repo"

✅ **User can only modify their own repositories**
- All write operations (add, update, delete files/folders) check ownership

✅ **Repository ownership is immutable**
- `ownerId` is set at creation and never changes

✅ **Authentication is required for all API endpoints**
- Configured in `SecurityConfiguration.java`

## Example Scenarios

### Scenario 1: Valid Repository Creation
```
User JWT sub: "95b54e9e-bbe3-4675-9c12-790ccf41dbb3"
Request: POST /api/repositories
Body: {"identifier": "95b54e9e-bbe3-4675-9c12-790ccf41dbb3/my-project"}
Result: ✅ Repository created successfully
```

### Scenario 2: Invalid Repository Creation (Wrong Owner)
```
User JWT sub: "95b54e9e-bbe3-4675-9c12-790ccf41dbb3"
Request: POST /api/repositories
Body: {"identifier": "other-user-id/my-project"}
Result: ❌ 403 Forbidden - "You can only create repositories under your own username"
```

### Scenario 3: Unauthorized File Modification
```
User JWT sub: "95b54e9e-bbe3-4675-9c12-790ccf41dbb3"
Request: POST /api/repositories/other-user-id/their-repo/files
Result: ❌ 403 Forbidden - "You don't have permission to modify this repository"
```

### Scenario 4: Reading Someone Else's Repository
```
User JWT sub: "95b54e9e-bbe3-4675-9c12-790ccf41dbb3"
Request: GET /api/repositories/other-user-id/their-repo/browse
Result: ✅ Directory contents returned (read operations are public)
```

## JWT Token Format

Example JWT payload from Keycloak:
```json
{
  "exp": 1766999207,
  "iat": 1766998907,
  "jti": "e008bf1f-0b8e-48b2-bab6-2fb556d8497c",
  "iss": "http://localhost:6969/realms/fdsa",
  "sub": "95b54e9e-bbe3-4675-9c12-790ccf41dbb3",  // ← User ID used for authorization
  "preferred_username": "012e",
  "email": "huyphmnhat@gmail.com",
  "name": "Huy Pham Nhat"
}
```

The `sub` claim is the primary identifier used for all authorization checks.

## Testing

### Manual Testing with cURL

1. Get JWT token from Keycloak:
```bash
TOKEN=$(curl -X POST http://localhost:6969/realms/fdsa/protocol/openid-connect/token \
  -d "client_id=fdsa-backend" \
  -d "grant_type=password" \
  -d "username=YOUR_USERNAME" \
  -d "password=YOUR_PASSWORD" \
  | jq -r '.access_token')
```

2. Create repository:
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"YOUR_USER_ID/test-repo","description":"Test"}'
```

3. Try to create repository with wrong owner (should fail):
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"wrong-user-id/test-repo","description":"Test"}'
```

## Database Migration

If you have existing repositories without `ownerId`, you'll need to migrate them:

```sql
-- Extract owner from identifier and set ownerId
UPDATE repositories 
SET owner_id = SUBSTRING(identifier FROM '^([^/]+)/');
```

## Future Enhancements

- [ ] Add collaborator support (multiple users can access a repository)
- [ ] Add organization support (teams of users owning repositories)
- [ ] Add public/private repository visibility
- [ ] Add read permissions (currently all reads are allowed)
- [ ] Add audit logging for authorization failures
- [ ] Add rate limiting per user

