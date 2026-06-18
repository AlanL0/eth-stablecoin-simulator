import type { components as AgentComponents, paths as AgentPaths } from "./generated/agent-api";
import type { components as JavaComponents, paths as JavaPaths } from "./generated/java-api";

export type SimulationRequest = JavaComponents["schemas"]["SimulationRequest"];
export type SimulationResponse = JavaComponents["schemas"]["SimulationResponse"];
export type ChartSpecV1 = JavaComponents["schemas"]["ChartSpecV1"];
export type EthPriceResponse = JavaComponents["schemas"]["EthPriceResponse"];
export type WalletStablecoinsResponse = JavaComponents["schemas"]["WalletStablecoinsResponse"];
export type AuditResponse = JavaComponents["schemas"]["AuditResponse"];
export type ErrorResponse = JavaComponents["schemas"]["ErrorResponse"];

export type RecommendYieldRequest = AgentComponents["schemas"]["RecommendYieldRequest"];
export type RecommendYieldResponse = AgentComponents["schemas"]["RecommendYieldResponse"];
export type SummarizeAuditRequest = AgentComponents["schemas"]["SummarizeAuditRequest"];
export type SummarizeAuditResponse = AgentComponents["schemas"]["SummarizeAuditResponse"];

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details?: string[],
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const JAVA_BASE = process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

type FetchConfig = {
  baseUrl?: string;
  fetchImpl?: typeof fetch;
};

let javaBaseUrl = JAVA_BASE;
let fetchImpl: typeof fetch = fetch;

export function configureApiClient(config: FetchConfig = {}): void {
  if (config.baseUrl) {
    javaBaseUrl = config.baseUrl;
  }
  if (config.fetchImpl) {
    fetchImpl = config.fetchImpl;
  }
}

/** Agent endpoints are served by the Java service (Spring AI stub until ETH-T22). */
export function configureAgentClient(config: FetchConfig = {}): void {
  configureApiClient(config);
}

export function resetApiClientForTests(): void {
  javaBaseUrl = JAVA_BASE;
  fetchImpl = fetch;
}

async function requestJson<T>(
  base: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetchImpl(`${base}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let payload: ErrorResponse | undefined;
    try {
      payload = (await response.json()) as ErrorResponse;
    } catch {
      payload = undefined;
    }
    throw new ApiError(
      response.status,
      payload?.code ?? "HTTP_ERROR",
      payload?.message ?? `Request failed (${response.status})`,
      payload?.details,
    );
  }

  return (await response.json()) as T;
}

export async function getEthPrice(): Promise<EthPriceResponse> {
  return requestJson<EthPriceResponse>(javaBaseUrl, "/api/price/eth");
}

export async function runSimulation(body: SimulationRequest): Promise<SimulationResponse> {
  return requestJson<SimulationResponse>(javaBaseUrl, "/api/simulations", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getWalletStablecoins(address: string): Promise<WalletStablecoinsResponse> {
  return requestJson<WalletStablecoinsResponse>(
    javaBaseUrl,
    `/api/wallet/${encodeURIComponent(address)}/stablecoins`,
  );
}

export type AuditQuery = {
  from?: string;
  to?: string;
  token?: string;
  hideValues?: boolean;
};

export async function getAudit(address: string, query: AuditQuery = {}): Promise<AuditResponse> {
  const params = new URLSearchParams();
  if (query.from) params.set("from", query.from);
  if (query.to) params.set("to", query.to);
  if (query.token) params.set("token", query.token);
  if (query.hideValues) params.set("hideValues", "true");
  const suffix = params.size ? `?${params.toString()}` : "";
  return requestJson<AuditResponse>(
    javaBaseUrl,
    `/api/audit/${encodeURIComponent(address)}${suffix}`,
  );
}

export async function recommendYield(
  body: RecommendYieldRequest,
): Promise<RecommendYieldResponse> {
  return requestJson<RecommendYieldResponse>(javaBaseUrl, "/agent/recommend-yield", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function summarizeAudit(
  body: SummarizeAuditRequest,
): Promise<SummarizeAuditResponse> {
  return requestJson<SummarizeAuditResponse>(javaBaseUrl, "/agent/summarize-audit", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export type JavaApiPaths = JavaPaths;
export type AgentApiPaths = AgentPaths;