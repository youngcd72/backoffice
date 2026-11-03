package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.InterfaceRequest;
import kr.or.coder.biz.system.link.dto.InterfaceResponse;
import kr.or.coder.biz.system.link.dto.InterfaceSaveRequest;
import kr.or.coder.biz.system.link.mapper.InterfaceMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SYLK010 인터페이스 Service 구현 (TB_INFC_M)
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
public class InterfaceServiceImpl implements InterfaceService {

    private final InterfaceMapper interfaceMapper;
    private final LinkCacheEvictPublisher linkCacheEvictPublisher;

    /**
     * 조회 조건에 맞는 인터페이스 목록을 인터페이스ID 순으로 조회 (허용 기관 수 포함)
     * @param request 조회 조건 (인터페이스ID·명·URL 부분 일치, 사용여부)
     * @return 인터페이스 목록
     */
    @Override
    public List<InterfaceResponse> getInterfaceList(InterfaceRequest request) {
        return interfaceMapper.selectInterfaceList(request);
    }

    /**
     * 인터페이스를 등록 또는 수정하고 coder-api 캐시 삭제를 알린다
     * 본문로그를 남기지 않으면 마스킹 필드는 의미가 없으므로 비운다.
     * @param request 인터페이스 (isNew 로 등록/수정 구분)
     * @param usrId 저장 사용자ID
     * @throws BusinessException 신규인데 인터페이스ID가 있거나 (sy.link.infc.duplicate), 수정 대상이 없는 경우
     */
    @Override
    @Transactional
    public void saveInterface(InterfaceSaveRequest request, String usrId) {
        if (!"Y".equals(request.getBodyLogYn())) {
            request.setMaskFldCont(null);
        }
        if (Boolean.TRUE.equals(request.getIsNew())) {
            RowSaveUtil.checkNotExists(interfaceMapper.existsInterface(request.getInfcId()),
                    "sy.link.infc.duplicate", request.getInfcId());
            interfaceMapper.insertInterface(request, usrId);
        } else {
            RowSaveUtil.checkUpdated(interfaceMapper.updateInterface(request, usrId));
        }
        linkCacheEvictPublisher.publish("INFC:" + request.getInfcId());
    }
}
