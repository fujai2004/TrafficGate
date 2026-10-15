package com.trafficgate;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import io.grpc.*;
import io.grpc.inprocess.*;
import com.trafficgate.proto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="REDIS_URL",matches=".+")
class RedisLimiterTest {
    String url=System.getenv("REDIS_URL");
    @Test void sharedQuotaIsAtomicAcrossInstances() throws Exception {
        var policy=new Policy(10,.001);
        try(var a=new RedisLimiter(url,policy,Map.of());var b=new RedisLimiter(url,policy,Map.of())) {
            var executor=Executors.newFixedThreadPool(16);
            try {
                String client=UUID.randomUUID().toString();
                var tasks=new ArrayList<Callable<Boolean>>();
                for(int i=0;i<200;i++){var node=i%2==0?a:b;tasks.add(()->node.check(client,1).allowed());}
                long allowed=0;
                for(var f:executor.invokeAll(tasks))if(f.get())allowed++;
                assertEquals(10,allowed,"both instances must share exactly ten tokens");
                assertTrue(a.check(UUID.randomUUID().toString(),1).allowed(),"other clients are independent");
                var deny=b.check(client,1);
                assertFalse(deny.allowed());assertEquals(0,deny.remaining());assertTrue(deny.retryAfterMs()>0);
            } finally { executor.shutdownNow(); }
        }
    }
    @Test void refillsAndValidatesServerPolicies() throws Exception {
        String client=UUID.randomUUID().toString();
        try(var limiter=new RedisLimiter(url,new Policy(1,20),Map.of(client,new Policy(3,20)))) {
            assertEquals(0,limiter.check(client,3).remaining());
            assertThrows(IllegalArgumentException.class,()->limiter.check(client,4));
            assertThrows(IllegalArgumentException.class,()->limiter.check("",1));
            assertThrows(IllegalArgumentException.class,()->limiter.check(client,-1));
            Thread.sleep(200);
            assertTrue(limiter.check(client,3).allowed());
            assertTrue(limiter.check(UUID.randomUUID().toString(),0).allowed());
        }
    }
    @Test void grpcReturnsDecisionsAndValidationErrors() throws Exception {
        try(var limiter=new RedisLimiter(url,new Policy(1,.001),Map.of())) {
            String name=InProcessServerBuilder.generateName();
            var server=InProcessServerBuilder.forName(name).directExecutor().addService(new QuotaService(limiter,null)).build().start();
            var channel=InProcessChannelBuilder.forName(name).directExecutor().build();
            try {
                var stub=RateLimiterGrpc.newBlockingStub(channel);
                var req=CheckRequest.newBuilder().setClientId(UUID.randomUUID().toString()).build();
                assertTrue(stub.checkQuota(req).getAllowed());
                var denied=stub.checkQuota(req);
                assertFalse(denied.getAllowed());assertTrue(denied.getRetryAfterMs()>0);
                var error=assertThrows(StatusRuntimeException.class,()->stub.checkQuota(CheckRequest.getDefaultInstance()));
                assertEquals(Status.Code.INVALID_ARGUMENT,error.getStatus().getCode());
            } finally {channel.shutdownNow();server.shutdownNow();}
        }
    }
    @Test void idleBucketExpiresAfterRefillWindow() throws Exception {
        String client=UUID.randomUUID().toString();
        try(var limiter=new RedisLimiter(url,new Policy(1,1000),Map.of());
            var redis=new redis.clients.jedis.JedisPooled(java.net.URI.create(url))) {
            limiter.check(client,1);
            long ttl=redis.pttl("trafficgate:bucket:"+client);
            assertTrue(ttl>0&&ttl<=1001);
            Thread.sleep(1200);
            assertFalse(redis.exists("trafficgate:bucket:"+client));
            assertTrue(limiter.check(client,1).allowed());
        }
    }
}
