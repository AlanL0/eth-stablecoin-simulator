import "@testing-library/jest-dom/vitest";

// Recharts ResponsiveContainer needs layout dimensions in jsdom.
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = ResizeObserverMock;