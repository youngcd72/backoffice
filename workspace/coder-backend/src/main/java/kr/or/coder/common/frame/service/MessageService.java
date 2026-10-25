package kr.or.coder.common.frame.service;

import java.util.Locale;
import java.util.Map;

/**
 * 화면(coder-frontend)에서 사용할 다국어 메시지 전체 목록 조회 Service
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성 (구체 클래스 MessageService를 인터페이스와 구현으로 분리)
 * </pre>
 */
public interface MessageService {

    /**
     * 지정한 로케일의 메시지 전체를 키 순서로 조회 (기본 번들 위에 언어별 번들을 덮어씀)
     * @param locale 조회할 로케일 (ko, en)
     * @return 메시지 키 → 메시지 문구 맵
     */
    Map<String, String> getMessageList(Locale locale);
}
