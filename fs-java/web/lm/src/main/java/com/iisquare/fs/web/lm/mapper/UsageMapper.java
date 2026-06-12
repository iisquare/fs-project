package com.iisquare.fs.web.lm.mapper;

import java.util.List;
import java.util.Map;

public interface UsageMapper {

    List<Map<String, Object>> summary(Map<String, Object> params);

    List<Map<String, Object>> rank(Map<String, Object> params);

    List<Map<String, Object>> groupCount(Map<String, Object> params);

    List<Map<String, Object>> timelineByPlace(Map<String, Object> params);

    List<Map<String, Object>> timelineTokens(Map<String, Object> params);

}
