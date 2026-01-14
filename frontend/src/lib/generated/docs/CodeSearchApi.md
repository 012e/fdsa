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

let q: string; // (default to undefined)
let repositoryIdentifier: string; // (optional) (default to undefined)
let language: string; // (optional) (default to undefined)
let filePathPattern: string; // (optional) (default to undefined)
let page: number; // (optional) (default to 0)
let size: number; // (optional) (default to 10)
let highlight: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.searchCode(
    q,
    repositoryIdentifier,
    language,
    filePathPattern,
    page,
    size,
    highlight
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **q** | [**string**] |  | defaults to undefined|
| **repositoryIdentifier** | [**string**] |  | (optional) defaults to undefined|
| **language** | [**string**] |  | (optional) defaults to undefined|
| **filePathPattern** | [**string**] |  | (optional) defaults to undefined|
| **page** | [**number**] |  | (optional) defaults to 0|
| **size** | [**number**] |  | (optional) defaults to 10|
| **highlight** | [**string**] |  | (optional) defaults to undefined|


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

