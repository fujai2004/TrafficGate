# Load tests (k6)

**Status:** Placeholder

Planned **k6** scripts to validate TrafficGate under burst and sustained load.

## Target SLO (from design)

- **10K requests/second** sustained
- **Sub-5ms p99** latency for allow/deny checks
- Containerized deployment via `deploy/docker-compose.yml`

## Planned scripts

| Script | Purpose |
|--------|---------|
| `burst.js` | Spike traffic, measure denial rate and latency |
| `sustained.js` | Steady RPS, p95/p99 reporting |
| `fairness.js` | Many client IDs, verify per-client isolation |

Scripts will be added in Milestone 3. See [docs/roadmap.md](../docs/roadmap.md).
