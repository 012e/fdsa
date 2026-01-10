"use client";

import { AuthProviderProps } from "react-oidc-context";

const keycloakUrl = import.meta.env["KEYCLOAK_URL"] ?? "http://localhost:6969";
const realm = "fdsa";
const clientId = "fdsa-frontend";
const appUrl = import.meta.env["APP_URL"] ?? "http://localhost:3000";

export const oidcConfig: AuthProviderProps = {
  authority: `${keycloakUrl}/realms/${realm}`,
  client_id: clientId,
  redirect_uri: appUrl,
  post_logout_redirect_uri: appUrl,
  response_type: "code",
  scope: "openid profile email",
};
