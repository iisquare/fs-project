if #ARGV % 3 ~= 0 then
    return redis.error_reply("argv must be like: key1 value1 score1 ...")
end

local zSetKey = KEYS[1]
local hashKey = KEYS[2]

local count = 0
for i = 1, #ARGV, 3 do
    local key = ARGV[i]
    local value = ARGV[i + 1]
    local score = tonumber(ARGV[i + 2])

    if score == nil then
        return redis.error_reply("score must be a number: " .. tostring(ARGV[i + 2]))
    end

    redis.call('ZADD', zSetKey, score, key)
    redis.call('HSET', hashKey, key, value)
    count = count + 1
end

return count
