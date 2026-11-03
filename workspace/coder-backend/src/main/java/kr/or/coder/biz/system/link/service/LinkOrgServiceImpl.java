package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceSaveRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgSaveRequest;
import kr.or.coder.biz.system.link.mapper.InterfaceMapper;
import kr.or.coder.biz.system.link.mapper.LinkOrgMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SYLK010 연계기관·기관별 허용 인터페이스 Service 구현 (TB_LNK_ORG_M, TB_LNK_ORG_INFC_R)
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
@Service
@RequiredArgsConstructor
public class LinkOrgServiceImpl implements LinkOrgService {

    private final LinkOrgMapper linkOrgMapper;
    private final InterfaceMapper interfaceMapper;
    private final LinkCacheEvictPublisher linkCacheEvictPublisher;

    /**
     * 조회 조건에 맞는 연계기관 목록을 기관ID 순으로 조회 (사용 가능 키 수 포함)
     * @param request 조회 조건 (기관ID·기관명 부분 일치, 채널구분코드, 사용여부)
     * @return 연계기관 목록
     */
    @Override
    public List<LinkOrgResponse> getLinkOrgList(LinkOrgRequest request) {
        return linkOrgMapper.selectLinkOrgList(request);
    }

    /**
     * 연계기관 1건을 조회
     * @param lnkOrgId 연계기관ID
     * @return 연계기관
     * @throws BusinessException 연계기관이 없는 경우 (sy.link.org.not.found)
     */
    @Override
    public LinkOrgResponse getLinkOrg(String lnkOrgId) {
        LinkOrgResponse org = linkOrgMapper.selectLinkOrg(lnkOrgId);
        if (org == null) {
            throw new BusinessException("sy.link.org.not.found", lnkOrgId);
        }
        return org;
    }

    /**
     * 연계기관을 등록 또는 수정하고 coder-api 캐시 삭제를 알린다
     * @param request 연계기관 (isNew 로 등록/수정 구분)
     * @param usrId 저장 사용자ID
     * @throws BusinessException 사용종료일자가 시작일자보다 앞서거나, 신규인데 기관ID가 있거나, 수정 대상이 없는 경우
     */
    @Override
    @Transactional
    public void saveLinkOrg(LinkOrgSaveRequest request, String usrId) {
        if (StringUtils.hasText(request.getUseStDt()) && StringUtils.hasText(request.getUseEndDt())
                && request.getUseEndDt().compareTo(request.getUseStDt()) < 0) {
            throw new BusinessException("sy.link.validate.use.period");
        }
        if (Boolean.TRUE.equals(request.getIsNew())) {
            RowSaveUtil.checkNotExists(linkOrgMapper.existsLinkOrg(request.getLnkOrgId()),
                    "sy.link.org.duplicate", request.getLnkOrgId());
            linkOrgMapper.insertLinkOrg(request, usrId);
        } else {
            RowSaveUtil.checkUpdated(linkOrgMapper.updateLinkOrg(request, usrId));
        }
        linkCacheEvictPublisher.publish("ORG:" + request.getLnkOrgId());
    }

    /**
     * 전체 인터페이스를 인터페이스ID 순으로 조회하고 연계기관의 허용 여부를 붙인다
     * @param lnkOrgId 연계기관ID
     * @return 전체 인터페이스 + 허용 여부
     * @throws BusinessException 연계기관이 없는 경우 (sy.link.org.not.found)
     */
    @Override
    public List<LinkOrgInterfaceResponse> getLinkOrgInterfaceList(String lnkOrgId) {
        checkLinkOrgExists(lnkOrgId);
        return linkOrgMapper.selectLinkOrgInterfaceList(lnkOrgId);
    }

    /**
     * 연계기관의 허용 인터페이스를 목록으로 전체 교체하고 coder-api 캐시 삭제를 알린다
     * 목록의 인터페이스는 허용(없으면 등록), 목록에 없는 기존 허용은 사용 안 함(N)으로 바꾼다 (행은 지우지 않음)
     * @param lnkOrgId 연계기관ID
     * @param request 허용할 인터페이스ID 목록
     * @param usrId 저장 사용자ID
     * @throws BusinessException 연계기관이 없거나 (sy.link.org.not.found), 등록되지 않은 인터페이스가 있는 경우
     */
    @Override
    @Transactional
    public void saveLinkOrgInterfaceList(String lnkOrgId, LinkOrgInterfaceSaveRequest request, String usrId) {
        checkLinkOrgExists(lnkOrgId);
        List<String> infcIdList = request.getInfcIdList().stream().distinct().toList();
        if (!infcIdList.isEmpty()) {
            RowSaveUtil.checkExists(interfaceMapper.selectInterfaceCount(infcIdList) == infcIdList.size());
        }
        for (String infcId : infcIdList) {
            linkOrgMapper.insertLinkOrgInterface(lnkOrgId, infcId, usrId);
        }
        linkOrgMapper.updateLinkOrgInterfaceUnused(lnkOrgId, infcIdList, usrId);
        linkCacheEvictPublisher.publish("ORG_INFC:" + lnkOrgId);
    }

    /**
     * 연계기관이 있는지 확인
     * @param lnkOrgId 연계기관ID
     * @throws BusinessException 연계기관이 없는 경우 (sy.link.org.not.found)
     */
    private void checkLinkOrgExists(String lnkOrgId) {
        if (!linkOrgMapper.existsLinkOrg(lnkOrgId)) {
            throw new BusinessException("sy.link.org.not.found", lnkOrgId);
        }
    }
}
