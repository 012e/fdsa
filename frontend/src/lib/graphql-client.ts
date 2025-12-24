import { GraphQLClient } from 'graphql-request'

const GRAPHQL_ENDPOINT = 'http://localhost:8080/graphql'

const client = new GraphQLClient(GRAPHQL_ENDPOINT, {
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * Custom request function that handles TypedDocumentNode from graphql-codegen
 * Automatically converts document objects to strings
 */
export async function request<TResult = any, TVariables extends Record<string, any> = Record<string, any>>(
  document: any,
  variables?: TVariables
): Promise<TResult> {
  const query = typeof document === 'string' ? document : document.toString()
  return client.request<TResult>(query, variables)
}

export const graphqlClient = {
  request,
}

export { gql } from 'graphql-request'
