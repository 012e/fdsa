# CodeSearchRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**query** | **string** |  | [optional] [default to undefined]
**repositoryId** | **string** |  | [optional] [default to undefined]
**repositoryIdentifier** | **string** |  | [optional] [default to undefined]
**language** | **string** |  | [optional] [default to undefined]
**fileExtension** | **string** |  | [optional] [default to undefined]
**filePathPattern** | **string** |  | [optional] [default to undefined]
**page** | **number** |  | [optional] [default to undefined]
**size** | **number** |  | [optional] [default to undefined]
**highlightFields** | **Array&lt;string&gt;** |  | [optional] [default to undefined]

## Example

```typescript
import { CodeSearchRequest } from './api';

const instance: CodeSearchRequest = {
    query,
    repositoryId,
    repositoryIdentifier,
    language,
    fileExtension,
    filePathPattern,
    page,
    size,
    highlightFields,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
