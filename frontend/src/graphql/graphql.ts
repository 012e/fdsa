/* eslint-disable */
export type Maybe<T> = T | null;
export type InputMaybe<T> = T | null | undefined;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
};

export type CloneRepositoryInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  identifier: Scalars['String']['input'];
  sourceUrl: Scalars['String']['input'];
};

export type Mutation = {
  __typename?: 'Mutation';
  addRepositoryFile: Scalars['Boolean']['output'];
  cloneRepository: Repository;
  createRepository: Repository;
  createRepositoryFolder: Scalars['Boolean']['output'];
  createSnippet: Snippet;
  deleteRepositoryFile: Scalars['Boolean']['output'];
  deleteRepositoryFolder: Scalars['Boolean']['output'];
  deleteSnippet: Scalars['Boolean']['output'];
  updateRepositoryFile: Scalars['Boolean']['output'];
  updateSnippet: Snippet;
};


export type MutationAddRepositoryFileArgs = {
  input: RepositoryFileChangeInput;
};


export type MutationCloneRepositoryArgs = {
  input: CloneRepositoryInput;
};


export type MutationCreateRepositoryArgs = {
  input: RepositoryInput;
};


export type MutationCreateRepositoryFolderArgs = {
  input: RepositoryPathChangeInput;
};


export type MutationCreateSnippetArgs = {
  snippet?: InputMaybe<SnippetInput>;
};


export type MutationDeleteRepositoryFileArgs = {
  input: RepositoryPathChangeInput;
};


export type MutationDeleteRepositoryFolderArgs = {
  input: RepositoryPathChangeInput;
};


export type MutationDeleteSnippetArgs = {
  id: Scalars['ID']['input'];
};


export type MutationUpdateRepositoryFileArgs = {
  input: RepositoryFileChangeInput;
};


export type MutationUpdateSnippetArgs = {
  id: Scalars['ID']['input'];
  snippet?: InputMaybe<SnippetInput>;
};

export type Query = {
  __typename?: 'Query';
  listFilesByPath: Array<SnippetFile>;
  repositories: Array<Repository>;
  repositoriesByOwner: Array<Repository>;
  repository: Repository;
  snippetById: Snippet;
  snippetByPath: Snippet;
  snippets: Array<Snippet>;
  snippetsByOwner: Array<Snippet>;
};


export type QueryListFilesByPathArgs = {
  owner: Scalars['String']['input'];
  path: Scalars['String']['input'];
};


export type QueryRepositoriesByOwnerArgs = {
  owner: Scalars['String']['input'];
};


export type QueryRepositoryArgs = {
  identifier: Scalars['String']['input'];
};


export type QuerySnippetByIdArgs = {
  id: Scalars['ID']['input'];
};


export type QuerySnippetByPathArgs = {
  owner: Scalars['String']['input'];
  path: Scalars['String']['input'];
};


export type QuerySnippetsByOwnerArgs = {
  owner: Scalars['String']['input'];
};

/** Code repository backed by the repository service */
export type Repository = {
  __typename?: 'Repository';
  description?: Maybe<Scalars['String']['output']>;
  filesystemPath: Scalars['String']['output'];
  identifier: Scalars['String']['output'];
};

export type RepositoryFileChangeInput = {
  commitMessage: Scalars['String']['input'];
  content: Scalars['String']['input'];
  encoding?: InputMaybe<RepositoryFileEncoding>;
  path: Scalars['String']['input'];
  repositoryId: Scalars['ID']['input'];
};

export enum RepositoryFileEncoding {
  Base64 = 'BASE64',
  Text = 'TEXT'
}

export type RepositoryInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  identifier: Scalars['String']['input'];
};

export type RepositoryPathChangeInput = {
  commitMessage: Scalars['String']['input'];
  path: Scalars['String']['input'];
  repositoryId: Scalars['ID']['input'];
};

export type Snippet = {
  __typename?: 'Snippet';
  code: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  owner: Scalars['String']['output'];
  path: Scalars['String']['output'];
};

export type SnippetFile = {
  __typename?: 'SnippetFile';
  id?: Maybe<Scalars['ID']['output']>;
  isDirectory: Scalars['Boolean']['output'];
  owner: Scalars['String']['output'];
  path: Scalars['String']['output'];
};

export type SnippetInput = {
  code: Scalars['String']['input'];
  owner: Scalars['String']['input'];
  path: Scalars['String']['input'];
};

export class TypedDocumentString<TResult, TVariables>
  extends String
  implements DocumentTypeDecoration<TResult, TVariables>
{
  __apiType?: NonNullable<DocumentTypeDecoration<TResult, TVariables>['__apiType']>;
  private value: string;
  public __meta__?: Record<string, any> | undefined;

  constructor(value: string, __meta__?: Record<string, any> | undefined) {
    super(value);
    this.value = value;
    this.__meta__ = __meta__;
  }

  override toString(): string & DocumentTypeDecoration<TResult, TVariables> {
    return this.value;
  }
}
