import { useAuth } from "react-oidc-context";
import { useSetAtom } from "jotai";
import { accessTokenAtom } from "@/lib/auth-atoms";
import { useEffect } from "react";

export function AuthSync({ children }: { children: React.ReactNode }) {
  const auth = useAuth();
  const setAccessToken = useSetAtom(accessTokenAtom);

  useEffect(() => {
    if (auth?.user?.access_token) {
      setAccessToken(auth?.user.access_token);
      console.log("token", auth.user.access_token);
    }
  }, [auth.user?.access_token, setAccessToken]);

  return <>{children}</>;
}
