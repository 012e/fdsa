import createClient from "openapi-fetch";
import type { paths } from "@/lib/generated/api-schema";
import createQueryClient from "openapi-react-query";

const api = createClient<paths>({ baseUrl: "http://localhost:8080" });
const queryApi = createQueryClient(api);

// const accessTokenInjectMiddleware: Middleware = {
//   async onRequest({ request }) {
//     const accessToken = store.get(accessTokenAtom);
//     if (accessToken) {
//       request.headers.set("Authorization", `Bearer ${accessToken}`);
//     }
//     return request;
//   },
//
//   async onResponse({ response, request }) {
//     // 1. Check for Unauthorized/Forbidden status
//     // Note: Axios code checked 403, your target code checked 401.
//     // I've included both to be safe.
//     if (response.status === 401 || response.status === 403) {
//       const refresher = store.get(getAccessTokenFnAtom);
//       console.log("refresher", refresher);
//
//       // Ensure we haven't already retried this specific request to prevent loops
//       // (We detect this by checking if the Authorization header matches the *current* store token
//       // or simply by trusting the refresher not to fail infinitely)
//       if (refresher) {
//         try {
//           // 2. Call the refresh function
//           const newAccessToken = await refresher();
//
//           if (newAccessToken) {
//             // 3. Create a new request based on the original
//             // We clone the headers to ensure we don't mutate the old request incorrectly
//             const newHeaders = new Headers(request.headers);
//             newHeaders.set("Authorization", `Bearer ${newAccessToken}`);
//
//             // 4. Retry the request with the new token
//             const retryResponse = await fetch(request.url, {
//               ...request,
//               method: request.method,
//               headers: newHeaders,
//               body: request.body,
//             });
//
//             return retryResponse;
//           }
//         } catch (refreshError) {
//           // If refresh fails, we fall through to return the original error response
//           console.error("Token refresh failed", refreshError);
//         }
//       }
//     }
//
//     // 5. Global Error Handling (from your target code)
//     if (response.status >= 400) {
//       // Optional: Parse JSON safely to avoid crashing if response isn't JSON
//       let errorMessage = "Unknown error";
//       try {
//         const errorData = await response.clone().json();
//         errorMessage = errorData.message || errorMessage;
//       } catch (e) {
//         errorMessage = await response.text();
//       }
//
//       throw new Error(`Error happened, detail: ${errorMessage}`);
//     }
//
//     return response;
//   },
// };
//
// api.use(accessTokenInjectMiddleware);

export default api;
export { queryApi };
