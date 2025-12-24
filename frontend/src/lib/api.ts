import { RepositoryControllerApi } from "@/lib/generated";
import axios from "axios";

const basePath = "http://localhost:8080";
const axiosClient = axios.create({
  baseURL: basePath,
});

const repositoryApi = new RepositoryControllerApi(
  undefined,
  basePath,
  axiosClient,
);

export { repositoryApi };
