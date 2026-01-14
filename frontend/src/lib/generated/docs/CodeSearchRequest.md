# CodeSearchRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**query** | **string** |  | [optional] [default to undefined]
**queryEmbedding** | **Array&lt;number&gt;** |  | [optional] [default to undefined]
**repositoryIdentifier** | **string** |  | [optional] [default to undefined]
**language** | **string** |  | [optional] [default to undefined]
**filePathPattern** | **string** |  | [optional] [default to undefined]
**page** | **number** |  | [optional] [default to undefined]
**size** | **number** |  | [optional] [default to undefined]
**highlightFields** | **Array&lt;string&gt;** |  | [optional] [default to undefined]

## Example

```typescript
import { CodeSearchRequest } from './api';

const instance: CodeSearchRequest = {
    query,
    queryEmbedding,
    repositoryIdentifier,
    language,
    filePathPattern,
    page,
    size,
    highlightFields,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
