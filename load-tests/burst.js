// TrafficGate k6 burst test — PLACEHOLDER
// Run when gRPC endpoint is implemented:
//   k6 run load-tests/burst.js

import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 50,
  duration: "30s",
  thresholds: {
    // Target: p(99) < 5ms once wired to real gRPC/HTTP gateway
    http_req_duration: ["p(99)<5000"],
  },
};

export default function () {
  // TODO: replace with gRPC client calls to rate-limiter-service
  const res = http.get("http://localhost:8080/health");
  check(res, { "placeholder status 200": (r) => r.status === 200 || r.status === 404 });
  sleep(0.01);
}
