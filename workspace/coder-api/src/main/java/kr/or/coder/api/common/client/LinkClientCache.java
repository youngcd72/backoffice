package kr.or.coder.api.common.client;

import kr.or.coder.api.common.client.mapper.LinkClientMapper;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.api.common.support.TtlCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;

/**
 * 연계키·인터페이스·연계기관 허용 인터페이스를 인스턴스 메모리에 캐시한다 (coder.api.cache.ttl-seconds).
 * coder-backend 가 연계 정보를 바꾸면 Redis 채널(coder.api.cache.evict-channel)로 알리고, 모든 인스턴스가 캐시를 비운다.
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
public class LinkClientCache {

    private final LinkClientMapper linkClientMapper;
    private final TtlCache<String, LinkClient> clientCache;
    private final TtlCache<String, LinkInterface> interfaceCache;
    private final TtlCache<String, Set<String>> allowedInfcCache;

    /**
     * 매퍼와 캐시 설정으로 생성
     * @param linkClientMapper 연계키·인터페이스 조회 매퍼
     * @param apiProperties 대외 연계 API 설정
     * @param clock 시간 기준 Clock
     */
    public LinkClientCache(LinkClientMapper linkClientMapper, ApiProperties apiProperties, Clock clock) {
        this.linkClientMapper = linkClientMapper;
        long ttlSeconds = apiProperties.getCache().getTtlSeconds();
        this.clientCache = new TtlCache<>(ttlSeconds, clock);
        this.interfaceCache = new TtlCache<>(ttlSeconds, clock);
        this.allowedInfcCache = new TtlCache<>(ttlSeconds, clock);
    }

    /**
     * 키ID로 연계키 정보를 조회
     * @param keyId 키ID (client_id)
     * @return 연계키 정보 (없으면 null)
     */
    public LinkClient getClient(String keyId) {
        return clientCache.get(keyId, linkClientMapper::selectLinkClient);
    }

    /**
     * 인터페이스ID로 인터페이스 정보를 조회
     * @param infcId 인터페이스ID
     * @return 인터페이스 정보 (없으면 null)
     */
    public LinkInterface getInterface(String infcId) {
        return interfaceCache.get(infcId, linkClientMapper::selectLinkInterface);
    }

    /**
     * 연계기관에 허용된 사용 중인 인터페이스ID 목록을 조회
     * @param lnkOrgId 연계기관ID
     * @return 인터페이스ID 집합
     */
    public Set<String> getAllowedInfcIds(String lnkOrgId) {
        return allowedInfcCache.get(lnkOrgId, id -> Set.copyOf(linkClientMapper.selectAllowedInfcIdList(id)));
    }

    /**
     * 캐시를 모두 비운다 (캐시 삭제 채널 수신 시)
     */
    public void evictAll() {
        clientCache.clear();
        interfaceCache.clear();
        allowedInfcCache.clear();
        log.info("link client cache evicted");
    }
}
