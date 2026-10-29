package kr.or.coder.api.common.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 인터페이스 정보 (TB_INFC_M)
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
@Getter
@Setter
public class LinkInterface {

    private String infcId;
    private String infcNm;
    private String httpMethNm;
    private String url;
    private String bodyLogYn;
    private String maskFldCont;
    private String useYn;

    /**
     * 요청·응답 본문을 호출 이력에 남기는지 확인
     * @return 본문로그여부가 Y 면 true
     */
    public boolean isBodyLog() {
        return "Y".equals(bodyLogYn);
    }

    /**
     * 마스킹필드내용(쉼표로 구분한 필드명/JSON 경로)을 목록으로 반환
     * @return 마스킹 필드 목록 (없으면 빈 목록)
     */
    public List<String> getMaskFieldList() {
        if (!StringUtils.hasText(maskFldCont)) {
            return List.of();
        }
        return Arrays.stream(maskFldCont.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
