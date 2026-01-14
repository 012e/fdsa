import { Scripts, createRootRoute } from "@tanstack/react-router";
import { NuqsAdapter } from 'nuqs/adapters/tanstack-router'
import { TanStackRouterDevtoolsPanel } from "@tanstack/react-router-devtools";
import { TanStackDevtools } from "@tanstack/react-devtools";
import { AuthProvider } from "react-oidc-context";
import { Provider as JotaiProvider } from "jotai";
import store from "@/lib/store";

import Header from "@/components/Header";
import { AuthSync } from "@/components/AuthSync";
import { oidcConfig } from "@/lib/auth-config";

import TanStackQueryDevtools from "../integrations/tanstack-query/devtools";

import appCss from "../styles.css?url";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";

export const Route = createRootRoute({
  head: () => ({
    meta: [
      {
        charSet: "utf-8",
      },
      {
        name: "viewport",
        content: "width=device-width, initial-scale=1",
      },
      {
        title: "TanStack Start Starter",
      },
    ],
    links: [
      {
        rel: "stylesheet",
        href: appCss,
      },
    ],
  }),
  ssr: false,

  shellComponent: RootDocument,
});

const client = new QueryClient();
function RootDocument({ children }: { children: React.ReactNode }) {
  return (
    <>
      <NuqsAdapter>
        <AuthProvider {...oidcConfig}>
          <QueryClientProvider client={client}>
            <JotaiProvider store={store}>
              <AuthSync>
                <Header />
                {children}
                <TanStackDevtools
                  config={{
                    position: "bottom-right",
                  }}
                  plugins={[
                    {
                      name: "Tanstack Router",
                      render: <TanStackRouterDevtoolsPanel />,
                    },
                    TanStackQueryDevtools,
                  ]}
                />
              </AuthSync>
            </JotaiProvider>
          </QueryClientProvider>
        </AuthProvider>
        <Scripts />
        <Toaster richColors />
      </NuqsAdapter>
    </>
  );
}
