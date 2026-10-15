import grpc from "k6/net/grpc";
import { check } from "k6";
import { Rate } from "k6/metrics";
const errors = new Rate("quota_errors");
const client = new grpc.Client();
client.load(["../rate-limiter-service/src/main/proto"],"trafficgate.proto");
export const options = {
  scenarios: { traffic: { executor:"constant-arrival-rate",rate:Number(__ENV.RATE||1000),timeUnit:"1s",duration:__ENV.DURATION||"30s",preAllocatedVUs:100,maxVUs:500 } },
  thresholds: { grpc_req_duration:["p(99)<"+(__ENV.P99_MS||50)],quota_errors:["rate<0.01"],dropped_iterations:["count==0"] },
};
export default function () {
  if (__ITER===0) client.connect(__ENV.GRPC_ADDR||"localhost:50051",{plaintext:true});
  const response=client.invoke("trafficgate.v1.RateLimiter/CheckQuota",{clientId:"load-"+(__VU%100),cost:1});
  errors.add(response.status!==grpc.StatusOK);
  check(response,{"valid quota response":r=>r.status===grpc.StatusOK&&typeof r.message.allowed==="boolean"});
}
