package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceSaveRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgSaveRequest;

import java.util.List;

/**
 * SYLK010 연계기관·기관별 허용 인터페이스 Service
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
public interface LinkOrgService {

    /**
     * 조회 조건에 맞는 연계기관 목록을 조회 (사용 가능 키 수 포함)
     * @param request 조회 조건
     * @return 연계기관 목록
     */
    List<LinkOrgResponse> getLinkOrgList(LinkOrgRequest request);

    /**
     * 연계기관 1건을 조회
     * @param lnkOrgId 연계기관ID
     * @return 연계기관
     */
    LinkOrgResponse getLinkOrg(String lnkOrgId);

    /**
     * 연계기관을 등록 또는 수정
     * @param request 연계기관 (isNew 로 등록/수정 구분)
     * @param usrId 저장 사용자ID
     */
    void saveLinkOrg(LinkOrgSaveRequest request, String usrId);

    /**
     * 전체 인터페이스와 연계기관의 허용 여부를 조회
     * @param lnkOrgId 연계기관ID
     * @return 전체 인터페이스 + 허용 여부
     */
    List<LinkOrgInterfaceResponse> getLinkOrgInterfaceList(String lnkOrgId);

    /**
     * 연계기관의 허용 인터페이스를 목록으로 전체 교체 (목록에 없는 기존 허용은 사용 안 함으로 변경)
     * @param lnkOrgId 연계기관ID
     * @param request 허용할 인터페이스ID 목록
     * @param usrId 저장 사용자ID
     */
    void saveLinkOrgInterfaceList(String lnkOrgId, LinkOrgInterfaceSaveRequest request, String usrId);
}
