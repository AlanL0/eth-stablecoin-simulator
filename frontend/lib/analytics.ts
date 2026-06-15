export type AnalyticsEventName = "simulation_completed";

export type SimulationCompletedProperties = {
  protocol: string;
  riskTier: string;
  healthRatio: number;
  years: number;
};

type EventProperties = {
  simulation_completed: SimulationCompletedProperties;
};

type AnalyticsSink = (event: AnalyticsEventName, properties: EventProperties[AnalyticsEventName]) => void;

let sink: AnalyticsSink | null = null;
const recorded: Array<{ event: AnalyticsEventName; properties: EventProperties[AnalyticsEventName] }> = [];

export function setAnalyticsSink(next: AnalyticsSink | null): void {
  sink = next;
}

export function resetAnalyticsForTests(): void {
  sink = null;
  recorded.length = 0;
}

export function getRecordedEvents(): typeof recorded {
  return [...recorded];
}

export function trackEvent<T extends AnalyticsEventName>(
  event: T,
  properties: EventProperties[T],
): void {
  recorded.push({ event, properties });
  if (sink) {
    sink(event, properties);
  }
}