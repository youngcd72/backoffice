package kr.or.coder.api.common.calllog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CallLogMaskerTest {

    private final CallLogMasker masker = new CallLogMasker(new ObjectMapper());

    private String mask(String body, List<String> fields, int maxLength) {
        return masker.mask(body == null ? null : body.getBytes(StandardCharsets.UTF_8), fields, maxLength);
    }

    @Test
    void masksFieldNameAtAnyDepth() {
        String result = mask("{\"usrNm\":\"kim\",\"data\":{\"usrNm\":\"lee\",\"age\":3}}", List.of("usrNm"), 4000);

        assertThat(result).isEqualTo("{\"usrNm\":\"***\",\"data\":{\"usrNm\":\"***\",\"age\":3}}");
    }

    @Test
    void masksPathOnlyAndAppliesToArrayElements() {
        String body = "{\"usrNm\":\"kim\",\"data\":{\"items\":[{\"telNo\":\"010\"},{\"telNo\":\"011\"}]}}";

        String result = mask(body, List.of("$.data.items.telNo"), 4000);

        assertThat(result).isEqualTo("{\"usrNm\":\"kim\",\"data\":{\"items\":[{\"telNo\":\"***\"},{\"telNo\":\"***\"}]}}");
    }

    @Test
    void alwaysMasksSecretsEvenWithoutConfig() {
        String result = mask("{\"client_secret\":\"s\",\"password\":\"p\",\"ok\":1}", List.of(), 4000);

        assertThat(result).isEqualTo("{\"client_secret\":\"***\",\"password\":\"***\",\"ok\":1}");
    }

    @Test
    void truncatesToMaxLength() {
        String result = mask("{\"a\":\"0123456789\"}", List.of(), 10);

        assertThat(result).hasSize(10);
    }

    @Test
    void omitsNonJsonBodyWhenMaskFieldsConfigured() {
        assertThat(mask("name=kim", List.of("name"), 4000)).isEqualTo(CallLogMasker.NON_JSON_OMITTED);
        assertThat(mask("plain text", List.of(), 4000)).isEqualTo("plain text");
        assertThat(mask("password=1234", List.of(), 4000)).isEqualTo(CallLogMasker.NON_JSON_OMITTED);
    }

    @Test
    void returnsNullForEmptyBody() {
        assertThat(mask(null, List.of(), 4000)).isNull();
        assertThat(mask("", List.of(), 4000)).isNull();
    }
}
