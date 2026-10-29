package kr.or.coder.api.common.auth;

import kr.or.coder.api.common.auth.dto.ClientKeyIssueResult;

/**
 * 클라이언트 키 발급 Service (Phase 2 에서 coder-backend 연계 관리 API 로 옮긴다)
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
public interface ClientKeyIssueService {

    /**
     * 연계기관에 클라이언트 키를 발급한다. 비밀키는 KMS 로 암호화해 저장하고 원문은 결과로 한 번만 반환한다.
     * @param lnkOrgId 연계기관ID
     * @param expireDays 유효 기간 (일, 0 이하면 만료 없음)
     * @param regrId 등록자ID
     * @return 키ID, 비밀키 원문, 만료일시
     * @throws kr.or.coder.core.common.exception.BusinessException 연계기관이 없는 경우 (sy.api.link.org.not.found)
     */
    ClientKeyIssueResult issueClientKey(String lnkOrgId, int expireDays, String regrId);
}
