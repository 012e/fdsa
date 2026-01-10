import { RepositoryControllerApi, CodeSearchApi } from "@/lib/generated";
import axios from "axios";
import { getDefaultStore } from "jotai";
import { accessTokenAtom } from "./auth-atoms";

const basePath = "http://localhost:8080";
const axiosClient = axios.create({
  baseURL: basePath,
});

axiosClient.interceptors.request.use(
  (config) => {
    const store = getDefaultStore();
    const token = store.get(accessTokenAtom);

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

const repositoryApi = new RepositoryControllerApi(
  undefined,
  basePath,
  axiosClient,
);

const codeSearchApi = new CodeSearchApi(undefined, basePath, axiosClient);

export { repositoryApi, codeSearchApi };
