package kr.or.coder.api.common.calllog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * 호출 이력에 남길 요청·응답 본문을 마스킹하고 최대 길이로 자른다.
 * 마스킹 필드는 이름만 쓰면(usrNm) 모든 위치의 같은 이름 필드를, 점으로 이으면(data.usrNm, $.data.usrNm) 그 경로만 바꾼다.
 * 경로 중간의 배열은 모든 요소에 적용한다. 토큰·비밀키·비밀번호 필드는 설정과 관계없이 항상 마스킹한다.
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
@Component
@RequiredArgsConstructor
public class CallLogMasker {

    /** 마스킹 후 값 */
    static final String MASK = "***";
    /** JSON 이 아니라 마스킹할 수 없는 본문 대신 저장하는 값 */
    static final String NON_JSON_OMITTED = "[non-JSON body omitted]";
    /** 설정과 관계없이 항상 마스킹하는 필드 */
    private static final Set<String> ALWAYS_MASKED = Set.of(
            "access_token", "accessToken", "client_secret", "clientSecret", "password", "secret");

    private final ObjectMapper objectMapper;

    /**
     * 본문을 마스킹해 저장할 문자열로 만든다
     * @param body 본문 (UTF-8)
     * @param maskFields 마스킹 필드 목록
     * @param maxLength 최대 길이 (넘으면 자른다)
     * @return 저장할 본문 (본문이 없으면 null)
     */
    public String mask(byte[] body, List<String> maskFields, int maxLength) {
        if (body == null || body.length == 0) {
            return null;
        }
        String result;
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || root.isMissingNode()) {
                return null;
            }
            maskByName(root, ALWAYS_MASKED);
            for (String field : maskFields) {
                String path = field.startsWith("$.") ? field.substring(2) : field;
                if (path.contains(".")) {
                    maskByPath(root, path.split("\\."), 0);
                } else {
                    maskByName(root, Set.of(path));
                }
            }
            result = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            result = new String(body, StandardCharsets.UTF_8);
            if (!maskFields.isEmpty() || ALWAYS_MASKED.stream().anyMatch(result::contains)) {
                result = NON_JSON_OMITTED;
            }
        } catch (IOException e) {
            result = NON_JSON_OMITTED;
        }
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }

    /**
     * 모든 위치에서 이름이 같은 필드 값을 마스킹
     * @param node JSON 노드
     * @param names 마스킹할 필드명
     */
    private void maskByName(JsonNode node, Set<String> names) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(name -> {
                if (names.contains(name)) {
                    objectNode.put(name, MASK);
                }
            });
        }
        for (JsonNode child : node) {
            maskByName(child, names);
        }
    }

    /**
     * 경로에 해당하는 필드 값을 마스킹 (배열은 모든 요소에 적용)
     * @param node 현재 JSON 노드
     * @param segments 경로 조각
     * @param index 현재 경로 위치
     */
    private void maskByPath(JsonNode node, String[] segments, int index) {
        if (node.isArray()) {
            node.forEach(element -> maskByPath(element, segments, index));
            return;
        }
        if (!(node instanceof ObjectNode objectNode) || !objectNode.has(segments[index])) {
            return;
        }
        if (index == segments.length - 1) {
            objectNode.put(segments[index], MASK);
        } else {
            maskByPath(objectNode.get(segments[index]), segments, index + 1);
        }
    }
}
