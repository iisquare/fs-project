local zSetKey = KEYS[1]
local hashKey = KEYS[2]
local minScore = 0
local maxScore = tonumber(ARGV[1]) or 0
local limitOffset = 0
local limitCount = 1

local elements = redis.call('ZRANGEBYSCORE', zSetKey, minScore, maxScore, 'WITHSCORES', 'LIMIT', limitOffset, limitCount)

if #elements == 0 then return {} end

local key = elements[1]
local score = elements[2]
local value = redis.call('HGET', hashKey, key)

redis.call('ZREM', KEYS[1], key)
redis.call('HDEL', KEYS[2], key)

return { key, value, score }
