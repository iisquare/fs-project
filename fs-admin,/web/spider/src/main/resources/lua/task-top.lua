local zSetKey = KEYS[1]
local hashKey = KEYS[2]
local startOffset = 0
local stopOffset = 0

local elements = redis.call('ZRANGE', zSetKey, startOffset, stopOffset, 'WITHSCORES')

if #elements == 0 then return {} end

local key = elements[1]
local score = elements[2]
local value = redis.call('HGET', hashKey, key)

return { key, value, score }
