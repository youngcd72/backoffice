package kr.or.coder.api.common.support;

import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.config.ApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 카운터로 연계키별 호출 한도(분당·일·동시)를 센다. 인스턴스가 여러 개여도 한도가 함께 적용된다.
 * - 분당: api:rl:{KEY_ID}:mi:{yyyyMMddHHmm} 고정 윈도 (TTL 2분)
 * - 일  : api:rl:{KEY_ID}:day:{yyyyMMdd} (TTL 2일)
 * - 동시: api:rl:{KEY_ID}:simu 요청 시작 시 증가, 종료 시 감소 (비정상 종료 대비 TTL)
 * 한도 값이 비어 있으면(null) 제한하지 않는다.
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@Component
public class RateLimiter {

    /** 분당·일 카운터를 한 번에 증가시키고, 처음 만든 키에만 TTL 을 건다 */
    private static final RedisScript<List> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local counts = {}
            for i, key in ipairs(KEYS) do
              local count = redis.call('INCR', key)
              if count == 1 then redis.call('EXPIRE', key, ARGV[i]) end
              counts[i] = count
            end
            return counts
            """, List.class);

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String MINUTE_TTL_SECONDS = "120";
    private static final String DAY_TTL_SECONDS = "172800";
    private static final String DENY = "deny";

    private final StringRedisTemplate redisTemplate;
    private final ApiProperties.RateLimit properties;
    private final Clock clock;

    /**
     * Redis 와 호출 한도 설정으로 생성
     * @param redisTemplate 문자열 RedisTemplate
     * @param apiProperties 대외 연계 API 설정
     * @param clock 시간 기준 Clock
     */
    public RateLimiter(StringRedisTemplate redisTemplate, ApiProperties apiProperties, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = apiProperties.getRateLimit();
        this.clock = clock;
    }

    /**
     * 호출 한도를 확인하고 카운터를 증가 (동시 한도를 넘으면 증가한 동시 카운터는 되돌린다)
     * @param client 연계키 정보
     * @return 확인 결과
     */
    public RateLimitResult acquire(LinkClient client) {
        Integer miLimit = client.getMiCallLmtCnt();
        Integer dayLimit = client.getDayCallLmtCnt();
        Integer simuLimit = client.getSimuRqmtLmtCnt();
        if (miLimit == null && dayLimit == null && simuLimit == null) {
            return RateLimitResult.UNLIMITED;
        }

        try {
            LocalDateTime now = LocalDateTime.now(clock);
            String prefix = "api:rl:" + client.getKeyId() + ":";
            List<String> keys = new ArrayList<>();
            List<String> ttls = new ArrayList<>();
            if (miLimit != null) {
                keys.add(prefix + "mi:" + now.format(MINUTE));
                ttls.add(MINUTE_TTL_SECONDS);
            }
            if (dayLimit != null) {
                keys.add(prefix + "day:" + now.format(DAY));
                ttls.add(DAY_TTL_SECONDS);
            }
            List<?> counts = keys.isEmpty() ? List.of()
                    : redisTemplate.execute(INCREMENT_SCRIPT, keys, ttls.toArray());
            long miCount = miLimit != null ? ((Number) counts.get(0)).longValue() : 0;
            long dayCount = dayLimit != null ? ((Number) counts.get(counts.size() - 1)).longValue() : 0;

            Integer headerLimit = miLimit != null ? miLimit : dayLimit;
            long remaining = headerLimit == null ? 0 : Math.max(0, headerLimit - (miLimit != null ? miCount : dayCount));

            if (miLimit != null && miCount > miLimit) {
                return RateLimitResult.exceeded(headerLimit, remaining, "minute", 60 - now.getSecond());
            }
            if (dayLimit != null && dayCount > dayLimit) {
                long untilMidnight = Duration.between(now, now.truncatedTo(ChronoUnit.DAYS).plusDays(1)).toSeconds();
                return RateLimitResult.exceeded(headerLimit, remaining, "day", Math.max(1, untilMidnight));
            }
            if (simuLimit != null) {
                String simuKey = prefix + "simu";
                Long running = redisTemplate.opsForValue().increment(simuKey);
                redisTemplate.expire(simuKey, Duration.ofSeconds(properties.getSimuTtlSeconds()));
                if (running != null && running > simuLimit) {
                    redisTemplate.opsForValue().decrement(simuKey);
                    return RateLimitResult.exceeded(headerLimit, remaining, "concurrent", 1);
                }
                return new RateLimitResult(headerLimit, remaining, null, 0, true, false);
            }
            return new RateLimitResult(headerLimit, remaining, null, 0, false, false);
        } catch (RuntimeException e) {
            log.warn("rate limit store unavailable ({}): {}", properties.getRedisFailureMode(), e.getMessage());
            return DENY.equalsIgnoreCase(properties.getRedisFailureMode()) ? RateLimitResult.UNAVAILABLE : RateLimitResult.UNLIMITED;
        }
    }

    /**
     * 요청이 끝나면 동시 요청 카운터를 줄인다
     * @param client 연계키 정보
     */
    public void release(LinkClient client) {
        try {
            redisTemplate.opsForValue().decrement("api:rl:" + client.getKeyId() + ":simu");
        } catch (RuntimeException e) {
            log.warn("failed to release concurrent request counter: {}", e.getMessage());
        }
    }

    /**
     * 호출 한도 확인 결과
     * @param limit 응답 헤더 X-RateLimit-Limit 값 (분당 한도, 없으면 일 한도, 둘 다 없으면 null)
     * @param remaining 응답 헤더 X-RateLimit-Remaining 값
     * @param exceeded 초과한 한도 (minute / day / concurrent, 초과하지 않았으면 null)
     * @param retryAfterSeconds 다시 호출할 수 있을 때까지 남은 시간 (초)
     * @param simuAcquired 동시 요청 카운터를 증가했는지 여부 (요청 종료 시 release 필요)
     * @param unavailable Redis 장애로 거부해야 하는지 여부
     */
    public record RateLimitResult(Integer limit, long remaining, String exceeded, long retryAfterSeconds,
                                  boolean simuAcquired, boolean unavailable) {

        static final RateLimitResult UNLIMITED = new RateLimitResult(null, 0, null, 0, false, false);
        static final RateLimitResult UNAVAILABLE = new RateLimitResult(null, 0, null, 0, false, true);

        /**
         * 한도 초과 결과를 만든다
         * @param limit 헤더 한도
         * @param remaining 헤더 남은 횟수
         * @param exceeded 초과한 한도
         * @param retryAfterSeconds 다시 호출할 수 있을 때까지 남은 시간 (초)
         * @return 한도 초과 결과
         */
        static RateLimitResult exceeded(Integer limit, long remaining, String exceeded, long retryAfterSeconds) {
            return new RateLimitResult(limit, remaining, exceeded, retryAfterSeconds, false, false);
        }
    }
}
