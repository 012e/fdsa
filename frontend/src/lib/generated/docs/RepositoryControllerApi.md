# RepositoryControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**addRepositoryFile**](#addrepositoryfile) | **POST** /api/repositories/{owner}/{repository}/files | Add a file to a repository|
|[**cloneRepository**](#clonerepository) | **POST** /api/repositories/clone | Clone a repository from a source URL|
|[**createRepository**](#createrepository) | **POST** /api/repositories | Create a new repository|
|[**createRepositoryFolder**](#createrepositoryfolder) | **POST** /api/repositories/{owner}/{repository}/folders | Create a folder in a repository|
|[**deleteRepositoryFile**](#deleterepositoryfile) | **DELETE** /api/repositories/{owner}/{repository}/files | Delete a file from a repository|
|[**deleteRepositoryFolder**](#deleterepositoryfolder) | **DELETE** /api/repositories/{owner}/{repository}/folders | Delete a folder from a repository|
|[**getAllRepositories**](#getallrepositories) | **GET** /api/repositories | List all repositories|
|[**getRepositoriesByOwner**](#getrepositoriesbyowner) | **GET** /api/repositories/by-owner/{owner} | List repositories by owner|
|[**getRepository**](#getrepository) | **GET** /api/repositories/{owner}/{repository} | Get a repository by owner and name|
|[**listRepositoryDirectory**](#listrepositorydirectory) | **GET** /api/repositories/{owner}/{repository}/browse | List contents of a directory in a repository|
|[**readRepositoryFile**](#readrepositoryfile) | **GET** /api/repositories/{owner}/{repository}/files | Read contents of a file in a repository|
|[**updateRepositoryFile**](#updaterepositoryfile) | **PUT** /api/repositories/{owner}/{repository}/files | Update a file in a repository|

# **addRepositoryFile**
> boolean addRepositoryFile(repositoryFileChangeInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryFileChangeInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let repositoryFileChangeInput: RepositoryFileChangeInput; //

const { status, data } = await apiInstance.addRepositoryFile(
    owner,
    repository,
    repositoryFileChangeInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryFileChangeInput** | **RepositoryFileChangeInput**|  | |
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**boolean**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | File added |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cloneRepository**
> Repository cloneRepository(cloneRepositoryInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    CloneRepositoryInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let cloneRepositoryInput: CloneRepositoryInput; //

const { status, data } = await apiInstance.cloneRepository(
    cloneRepositoryInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **cloneRepositoryInput** | **CloneRepositoryInput**|  | |


### Return type

**Repository**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Repository cloned |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createRepository**
> Repository createRepository(repositoryInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let repositoryInput: RepositoryInput; //

const { status, data } = await apiInstance.createRepository(
    repositoryInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryInput** | **RepositoryInput**|  | |


### Return type

**Repository**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Repository created |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createRepositoryFolder**
> boolean createRepositoryFolder(repositoryPathChangeInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryPathChangeInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let repositoryPathChangeInput: RepositoryPathChangeInput; //

const { status, data } = await apiInstance.createRepositoryFolder(
    owner,
    repository,
    repositoryPathChangeInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryPathChangeInput** | **RepositoryPathChangeInput**|  | |
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**boolean**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Folder created |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteRepositoryFile**
> boolean deleteRepositoryFile(repositoryPathChangeInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryPathChangeInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let repositoryPathChangeInput: RepositoryPathChangeInput; //

const { status, data } = await apiInstance.deleteRepositoryFile(
    owner,
    repository,
    repositoryPathChangeInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryPathChangeInput** | **RepositoryPathChangeInput**|  | |
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**boolean**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | File deleted |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteRepositoryFolder**
> boolean deleteRepositoryFolder(repositoryPathChangeInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryPathChangeInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let repositoryPathChangeInput: RepositoryPathChangeInput; //

const { status, data } = await apiInstance.deleteRepositoryFolder(
    owner,
    repository,
    repositoryPathChangeInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryPathChangeInput** | **RepositoryPathChangeInput**|  | |
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**boolean**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Folder deleted |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAllRepositories**
> getAllRepositories()


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

const { status, data } = await apiInstance.getAllRepositories();
```

### Parameters
This endpoint does not have any parameters.


### Return type

void (empty response body)

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of repositories |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getRepositoriesByOwner**
> getRepositoriesByOwner()


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)

const { status, data } = await apiInstance.getRepositoriesByOwner(
    owner
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] | Repository owner | defaults to undefined|


### Return type

void (empty response body)

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of repositories for owner |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getRepository**
> Repository getRepository()


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)

const { status, data } = await apiInstance.getRepository(
    owner,
    repository
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**Repository**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Repository details |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listRepositoryDirectory**
> DirectoryContent listRepositoryDirectory()


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let path: string; //Directory path (empty or / for root) (optional) (default to '')

const { status, data } = await apiInstance.listRepositoryDirectory(
    owner,
    repository,
    path
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|
| **path** | [**string**] | Directory path (empty or / for root) | (optional) defaults to ''|


### Return type

**DirectoryContent**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Directory contents |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **readRepositoryFile**
> FileContent readRepositoryFile()


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let path: string; //File path (default to undefined)

const { status, data } = await apiInstance.readRepositoryFile(
    owner,
    repository,
    path
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|
| **path** | [**string**] | File path | defaults to undefined|


### Return type

**FileContent**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | File contents |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateRepositoryFile**
> boolean updateRepositoryFile(repositoryFileChangeInput)


### Example

```typescript
import {
    RepositoryControllerApi,
    Configuration,
    RepositoryFileChangeInput
} from './api';

const configuration = new Configuration();
const apiInstance = new RepositoryControllerApi(configuration);

let owner: string; //Repository owner (default to undefined)
let repository: string; //Repository name (default to undefined)
let repositoryFileChangeInput: RepositoryFileChangeInput; //

const { status, data } = await apiInstance.updateRepositoryFile(
    owner,
    repository,
    repositoryFileChangeInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **repositoryFileChangeInput** | **RepositoryFileChangeInput**|  | |
| **owner** | [**string**] | Repository owner | defaults to undefined|
| **repository** | [**string**] | Repository name | defaults to undefined|


### Return type

**boolean**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | File updated |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

