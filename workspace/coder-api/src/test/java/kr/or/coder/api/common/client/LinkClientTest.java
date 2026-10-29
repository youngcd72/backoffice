package kr.or.coder.api.common.client;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LinkClientTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 12, 0);

    private LinkClient usableClient() {
        LinkClient client = new LinkClient();
        client.setKeyStatCd(LinkClient.KEY_STAT_USE);
        client.setOrgUseYn("Y");
        return client;
    }

    @Test
    void usableWhenKeyAndOrgAreActive() {
        assertThat(usableClient().isUsable(NOW)).isTrue();
    }

    @Test
    void notUsableWhenKeyStoppedOrExpired() {
        LinkClient stopped = usableClient();
        stopped.setKeyStatCd("02");
        LinkClient expired = usableClient();
        expired.setExpiDttm(NOW);

        assertThat(stopped.isUsable(NOW)).isFalse();
        assertThat(expired.isUsable(NOW)).isFalse();
    }

    @Test
    void notUsableOutsideOrgUsePeriodOrWhenOrgStopped() {
        LinkClient notStarted = usableClient();
        notStarted.setUseStDt("20261028");
        LinkClient ended = usableClient();
        ended.setUseEndDt("20261026");
        LinkClient lastDay = usableClient();
        lastDay.setUseEndDt("20261027");
        LinkClient orgStopped = usableClient();
        orgStopped.setOrgUseYn("N");

        assertThat(notStarted.isUsable(NOW)).isFalse();
        assertThat(ended.isUsable(NOW)).isFalse();
        assertThat(lastDay.isUsable(NOW)).isTrue();
        assertThat(orgStopped.isUsable(NOW)).isFalse();
    }
}
