# CodeSearchApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**searchCode**](#searchcode) | **GET** /api/search/code | Search for code files|
|[**searchCodePost**](#searchcodepost) | **POST** /api/search/code | Search for code files (POST)|

# **searchCode**
> CodeSearchResponse searchCode()

Performs full-text search across indexed code files with optional filters

### Example

```typescript
import {
    CodeSearchApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CodeSearchApi(configuration);

let q: string; //Search query text (default to undefined)
let repositoryId: string; //Filter by repository ID (optional) (default to undefined)
let repositoryIdentifier: string; //Filter by repository identifier (owner/name) (optional) (default to undefined)
let language: string; //Filter by programming language (optional) (default to undefined)
let fileExtension: string; //Filter by file extension (optional) (default to undefined)
let filePathPattern: string; //Filter by file path pattern (supports wildcards) (optional) (default to undefined)
let page: number; //Page number (0-based) (optional) (default to 0)
let size: number; //Number of results per page (optional) (default to 10)
let highlight: string; //Fields to highlight (comma-separated) (optional) (default to undefined)

const { status, data } = await apiInstance.searchCode(
    q,
    repositoryId,
    repositoryIdentifier,
    language,
    fileExtension,
    filePathPattern,
    page,
    size,
    highlight
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **q** | [**string**] | Search query text | defaults to undefined|
| **repositoryId** | [**string**] | Filter by repository ID | (optional) defaults to undefined|
| **repositoryIdentifier** | [**string**] | Filter by repository identifier (owner/name) | (optional) defaults to undefined|
| **language** | [**string**] | Filter by programming language | (optional) defaults to undefined|
| **fileExtension** | [**string**] | Filter by file extension | (optional) defaults to undefined|
| **filePathPattern** | [**string**] | Filter by file path pattern (supports wildcards) | (optional) defaults to undefined|
| **page** | [**number**] | Page number (0-based) | (optional) defaults to 0|
| **size** | [**number**] | Number of results per page | (optional) defaults to 10|
| **highlight** | [**string**] | Fields to highlight (comma-separated) | (optional) defaults to undefined|


### Return type

**CodeSearchResponse**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Search results |  -  |
|**400** | Invalid search parameters |  -  |
|**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **searchCodePost**
> CodeSearchResponse searchCodePost(codeSearchRequest)

Performs full-text search across indexed code files with optional filters using POST request

### Example

```typescript
import {
    CodeSearchApi,
    Configuration,
    CodeSearchRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CodeSearchApi(configuration);

let codeSearchRequest: CodeSearchRequest; //

const { status, data } = await apiInstance.searchCodePost(
    codeSearchRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **codeSearchRequest** | **CodeSearchRequest**|  | |


### Return type

**CodeSearchResponse**

### Authorization

[Oauth2](../README.md#Oauth2)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Search results |  -  |
|**400** | Invalid search parameters |  -  |
|**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

