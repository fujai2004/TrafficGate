import assert from "node:assert/strict";
const urls=(process.env.LIMITER_URLS??"http://localhost:8081,http://localhost:8082").split(",");
for(const url of urls) {
  let ready=false;
  for(let i=0;i<90;i++){try{if((await fetch(url+"/ready")).ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,1000));}
  assert(ready,"limiter did not become ready: "+url);
}
const clientId="smoke-"+crypto.randomUUID();
const responses=await Promise.all(Array.from({length:40},(_,i)=>fetch(urls[i%urls.length]+"/v1/check",{
  method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({clientId})
})));
const values=await Promise.all(responses.map(r=>r.json()));
const allowed=values.filter(d=>d.allowed).length;
// The configured refill permits more tokens if the environment is slow.
assert(allowed>=10&&allowed<=12,"unexpected shared burst allowance: "+allowed);
assert(values.some(d=>!d.allowed&&d.retryAfterMs>0));
assert(values.every(d=>d.remaining>=0&&d.remaining<=10));
assert((await (await fetch(urls[0]+"/metrics")).text()).includes("trafficgate_decisions_total"));
assert.equal((await fetch(urls[0]+"/v1/check",{method:"POST",body:'{"clientId":""}'})).status,400);
console.log("Two instances share a quota; deny/retry, validation and metrics passed.");
