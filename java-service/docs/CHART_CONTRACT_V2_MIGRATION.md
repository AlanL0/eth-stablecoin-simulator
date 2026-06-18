# ChartSpecV1 → ChartContract v2 migration

ChartSpecV1 runtime models were removed in ETH-T18. All chart endpoints and simulation
responses now emit **ChartContract v2** (`schemaVersion: "2.0"`).

## Field mapping

| ChartSpecV1 | ChartContract v2 |
|---|---|
| `subtitle` | `description` |
| `chartType` | `assumptions.chartType` |
| `series[].name` | `series[].label` |
| `series[].points[]` | `series[].data[]` |
| `points[].y` / `y0` / `y1` | `data[].plotValue` + `displayValue` (+ band bounds in `metadata`) |
| `meta` | `assumptions` + `provenance.sources` |
| `source` | `provenance.builder` |
| `generatedAt` | `provenance.generatedAt` |
| `legend` | Frontend presentation only (not in v2 contract) |

## Semantics

- **`plotValue`**: server-rounded JSON number used only for chart coordinates.
- **`displayValue`**: exact authoritative decimal string for labels, tooltips, and tables.
- **TradFi labels**: e.g. health ratio → collateralization risk margin; liquidation price → collateral recovery threshold.

## Endpoints (unchanged paths)

- `GET /api/charts/simulation-projection`
- `GET /api/charts/liquidation-band`
- `GET /api/charts/health-ratio`
- `GET /api/charts/protocol-rates?asset=USDC` *(new)*
- `GET /api/charts/eth-price-history` *(new)*

No runtime dual-write or ChartSpecV1 compatibility shim is provided.