package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.LinkKeyHistResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueRequest;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyResponse;
import kr.or.coder.biz.system.link.dto.LinkKeySaveRequest;

import java.util.List;

/**
 * SYLK010 클라이언트 키 Service (coder-api OAuth2 client_id / client_secret)
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
public interface LinkKeyService {

    /**
     * 연계기관의 클라이언트 키 목록을 조회
     * @param lnkOrgId 연계기관ID
     * @return 클라이언트 키 목록
     */
    List<LinkKeyResponse> getLinkKeyList(String lnkOrgId);

    /**
     * 클라이언트 키를 발급 (비밀키 원문은 결과로 한 번만 반환)
     * @param lnkOrgId 연계기관ID
     * @param request 유효기간
     * @param usrId 발급 사용자ID
     * @return 키ID, 비밀키 원문, 만료일시
     */
    LinkKeyIssueResponse saveLinkKey(String lnkOrgId, LinkKeyIssueRequest request, String usrId);

    /**
     * 클라이언트 키의 상태·만료일·허용IP·호출 한도·비고를 수정
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @param request 수정 내용
     * @param usrId 수정 사용자ID
     */
    void saveLinkKeyDtl(String lnkOrgId, String keyId, LinkKeySaveRequest request, String usrId);

    /**
     * 클라이언트 키 이력을 조회
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @return 키 이력 목록 (최근 순)
     */
    List<LinkKeyHistResponse> getLinkKeyHistList(String lnkOrgId, String keyId);
}
