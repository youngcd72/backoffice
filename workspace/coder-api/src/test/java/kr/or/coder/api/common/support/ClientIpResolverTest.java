package kr.or.coder.api.common.support;

import kr.or.coder.api.common.config.ApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver resolver(String... trustedProxies) {
        ApiProperties properties = new ApiProperties();
        properties.setTrustedProxies(List.of(trustedProxies));
        return new ClientIpResolver(properties);
    }

    private MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void ignoresForwardedForFromUntrustedPeer() {
        assertThat(resolver("10.0.0.0/8").resolve(request("203.0.113.9", "1.2.3.4"))).isEqualTo("203.0.113.9");
    }

    @Test
    void takesRightmostUntrustedHopBehindTrustedProxies() {
        ClientIpResolver resolver = resolver("10.0.0.0/8");

        // 1.2.3.4 는 클라이언트가 위조해 넣은 값, 198.51.100.7 이 실제 클라이언트, 10.0.0.5 는 내부 프록시
        assertThat(resolver.resolve(request("10.0.0.1", "1.2.3.4, 198.51.100.7, 10.0.0.5"))).isEqualTo("198.51.100.7");
    }

    @Test
    void usesRemoteAddrWhenNoForwardedFor() {
        assertThat(resolver("10.0.0.0/8").resolve(request("10.0.0.1", null))).isEqualTo("10.0.0.1");
    }

    @Test
    void ignoresMalformedForwardedForHop() {
        assertThat(resolver("10.0.0.0/8").resolve(request("10.0.0.1", "not-an-ip"))).isEqualTo("not-an-ip");
    }

    @Test
    void allowListSupportsCidrAndBlankMeansNoRestriction() {
        assertThat(ClientIpResolver.isAllowed(null, "1.2.3.4")).isTrue();
        assertThat(ClientIpResolver.isAllowed(" ", "1.2.3.4")).isTrue();
        assertThat(ClientIpResolver.isAllowed("192.168.0.0/24, 10.1.1.1", "192.168.0.77")).isTrue();
        assertThat(ClientIpResolver.isAllowed("192.168.0.0/24, 10.1.1.1", "10.1.1.1")).isTrue();
        assertThat(ClientIpResolver.isAllowed("192.168.0.0/24, 10.1.1.1", "10.1.1.2")).isFalse();
        assertThat(ClientIpResolver.isAllowed("192.168.0.0/24", "not-an-ip")).isFalse();
    }
}
