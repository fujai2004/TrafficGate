-- Redis time keeps quota refill consistent across Java nodes.
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + tonumber(clock[2]) / 1000
local capacity, rate, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
local state = redis.call('HMGET', KEYS[1], 'tokens', 'updated')
local tokens, updated = tonumber(state[1]) or capacity, tonumber(state[2]) or now
tokens = math.min(capacity, tokens + math.max(0, now - updated) * rate / 1000)
local allowed, retry = 0, 0
if tokens >= cost then tokens = tokens - cost; allowed = 1
else retry = math.ceil((cost - tokens) * 1000 / rate) end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'updated', math.max(now, updated))
-- Expire only after an idle bucket would be full again.
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity * 1000 / rate) + 1000)
return {allowed, math.floor(tokens), retry}
