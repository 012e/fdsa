import { graphql } from '@/graphql'

// Queries
export const GET_REPOSITORIES = graphql(`
  query GetRepositories {
    repositories {
      identifier
      description
      filesystemPath
    }
  }
`)

export const GET_REPOSITORY = graphql(`
  query GetRepository($identifier: String!) {
    repository(identifier: $identifier) {
      identifier
      description
      filesystemPath
    }
  }
`)

export const GET_REPOSITORIES_BY_OWNER = graphql(`
  query GetRepositoriesByOwner($owner: String!) {
    repositoriesByOwner(owner: $owner) {
      identifier
      description
      filesystemPath
    }
  }
`)

// Mutations
export const CREATE_REPOSITORY = graphql(`
  mutation CreateRepository($input: RepositoryInput!) {
    createRepository(input: $input) {
      identifier
      description
      filesystemPath
    }
  }
`)

export const CLONE_REPOSITORY = graphql(`
  mutation CloneRepository($input: CloneRepositoryInput!) {
    cloneRepository(input: $input) {
      identifier
      description
      filesystemPath
    }
  }
`)

export const ADD_REPOSITORY_FILE = graphql(`
  mutation AddRepositoryFile($input: RepositoryFileChangeInput!) {
    addRepositoryFile(input: $input)
  }
`)

export const UPDATE_REPOSITORY_FILE = graphql(`
  mutation UpdateRepositoryFile($input: RepositoryFileChangeInput!) {
    updateRepositoryFile(input: $input)
  }
`)

export const DELETE_REPOSITORY_FILE = graphql(`
  mutation DeleteRepositoryFile($input: RepositoryPathChangeInput!) {
    deleteRepositoryFile(input: $input)
  }
`)

export const CREATE_REPOSITORY_FOLDER = graphql(`
  mutation CreateRepositoryFolder($input: RepositoryPathChangeInput!) {
    createRepositoryFolder(input: $input)
  }
`)

export const DELETE_REPOSITORY_FOLDER = graphql(`
  mutation DeleteRepositoryFolder($input: RepositoryPathChangeInput!) {
    deleteRepositoryFolder(input: $input)
  }
`)
