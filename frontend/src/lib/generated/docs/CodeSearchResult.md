# CodeSearchResult


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **string** |  | [optional] [default to undefined]
**repositoryId** | **string** |  | [optional] [default to undefined]
**repositoryIdentifier** | **string** |  | [optional] [default to undefined]
**filePath** | **string** |  | [optional] [default to undefined]
**fileName** | **string** |  | [optional] [default to undefined]
**fileExtension** | **string** |  | [optional] [default to undefined]
**language** | **string** |  | [optional] [default to undefined]
**content** | **string** |  | [optional] [default to undefined]
**size** | **number** |  | [optional] [default to undefined]
**score** | **number** |  | [optional] [default to undefined]
**highlights** | **{ [key: string]: Array&lt;string&gt;; }** |  | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**updatedAt** | **string** |  | [optional] [default to undefined]
**matchedChunks** | [**Array&lt;ChunkMatch&gt;**](ChunkMatch.md) |  | [optional] [default to undefined]

## Example

```typescript
import { CodeSearchResult } from './api';

const instance: CodeSearchResult = {
    id,
    repositoryId,
    repositoryIdentifier,
    filePath,
    fileName,
    fileExtension,
    language,
    content,
    size,
    score,
    highlights,
    createdAt,
    updatedAt,
    matchedChunks,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
