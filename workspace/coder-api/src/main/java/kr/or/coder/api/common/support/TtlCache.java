package kr.or.coder.api.common.support;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 유지 시간(TTL)이 있는 인스턴스 메모리 캐시 (값이 없다는 결과(null)도 캐시해 반복 조회를 막는다)
 * @param <K> 키 타입
 * @param <V> 값 타입
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
public class TtlCache<K, V> {

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final Clock clock;

    /**
     * 유지 시간과 시간 기준으로 캐시를 생성
     * @param ttlSeconds 유지 시간 (초)
     * @param clock 시간 기준 Clock
     */
    public TtlCache(long ttlSeconds, Clock clock) {
        this.ttlMillis = ttlSeconds * 1000L;
        this.clock = clock;
    }

    /**
     * 캐시 값을 반환하고, 없거나 만료되었으면 loader 로 조회해 저장
     * @param key 키
     * @param loader 원본 조회 함수 (null 반환 가능)
     * @return 캐시 값 (없으면 null)
     */
    public V get(K key, Function<K, V> loader) {
        long now = clock.millis();
        Entry<V> entry = entries.get(key);
        if (entry == null || entry.expireAt() <= now) {
            entry = new Entry<>(loader.apply(key), now + ttlMillis);
            entries.put(key, entry);
        }
        return entry.value();
    }

    /**
     * 캐시를 모두 비운다
     */
    public void clear() {
        entries.clear();
    }

    /**
     * 캐시 값과 만료 시각
     */
    private record Entry<V>(V value, long expireAt) {
    }
}
