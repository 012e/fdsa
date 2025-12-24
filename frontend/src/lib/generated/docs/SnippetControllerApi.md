# SnippetControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createSnippet**](#createsnippet) | **POST** /api/snippets | |
|[**deleteSnippet**](#deletesnippet) | **DELETE** /api/snippets/{id} | |
|[**getAllSnippets**](#getallsnippets) | **GET** /api/snippets | |
|[**getSnippet**](#getsnippet) | **GET** /api/snippets/{id} | |
|[**getSnippetByPath**](#getsnippetbypath) | **GET** /api/snippets/by-path | |
|[**getSnippetsByOwner**](#getsnippetsbyowner) | **GET** /api/snippets/by-owner/{owner} | |
|[**listFilesByPath**](#listfilesbypath) | **GET** /api/snippets/files | |
|[**updateSnippet**](#updatesnippet) | **PUT** /api/snippets/{id} | |

# **createSnippet**
> Snippet createSnippet(snippetInput)


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration,
    SnippetInput
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let snippetInput: SnippetInput; //

const { status, data } = await apiInstance.createSnippet(
    snippetInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **snippetInput** | **SnippetInput**|  | |


### Return type

**Snippet**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteSnippet**
> boolean deleteSnippet()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let id: string; // (default to undefined)

const { status, data } = await apiInstance.deleteSnippet(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**string**] |  | defaults to undefined|


### Return type

**boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAllSnippets**
> Array<Snippet> getAllSnippets()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

const { status, data } = await apiInstance.getAllSnippets();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<Snippet>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getSnippet**
> Snippet getSnippet()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let id: string; // (default to undefined)

const { status, data } = await apiInstance.getSnippet(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**string**] |  | defaults to undefined|


### Return type

**Snippet**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getSnippetByPath**
> Snippet getSnippetByPath()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let owner: string; // (default to undefined)
let path: string; // (default to undefined)

const { status, data } = await apiInstance.getSnippetByPath(
    owner,
    path
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] |  | defaults to undefined|
| **path** | [**string**] |  | defaults to undefined|


### Return type

**Snippet**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getSnippetsByOwner**
> Array<Snippet> getSnippetsByOwner()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let owner: string; // (default to undefined)

const { status, data } = await apiInstance.getSnippetsByOwner(
    owner
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] |  | defaults to undefined|


### Return type

**Array<Snippet>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listFilesByPath**
> Array<SnippetFile> listFilesByPath()


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let owner: string; // (default to undefined)
let path: string; // (default to undefined)

const { status, data } = await apiInstance.listFilesByPath(
    owner,
    path
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **owner** | [**string**] |  | defaults to undefined|
| **path** | [**string**] |  | defaults to undefined|


### Return type

**Array<SnippetFile>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateSnippet**
> Snippet updateSnippet(snippetInput)


### Example

```typescript
import {
    SnippetControllerApi,
    Configuration,
    SnippetInput
} from './api';

const configuration = new Configuration();
const apiInstance = new SnippetControllerApi(configuration);

let id: string; // (default to undefined)
let snippetInput: SnippetInput; //

const { status, data } = await apiInstance.updateSnippet(
    id,
    snippetInput
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **snippetInput** | **SnippetInput**|  | |
| **id** | [**string**] |  | defaults to undefined|


### Return type

**Snippet**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

