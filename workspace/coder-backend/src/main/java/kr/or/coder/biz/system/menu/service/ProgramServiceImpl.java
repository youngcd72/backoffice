package kr.or.coder.biz.system.menu.service;

import kr.or.coder.biz.system.menu.dto.ProgramComponentItemRequest;
import kr.or.coder.biz.system.menu.dto.ProgramComponentResponse;
import kr.or.coder.biz.system.menu.dto.ProgramComponentSaveRequest;
import kr.or.coder.biz.system.menu.dto.ProgramItemRequest;
import kr.or.coder.biz.system.menu.dto.ProgramRequest;
import kr.or.coder.biz.system.menu.dto.ProgramResponse;
import kr.or.coder.biz.system.menu.dto.ProgramSaveRequest;
import kr.or.coder.biz.system.menu.mapper.ProgramMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SYPG010 프로그램관리 Service 구현
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramServiceImpl implements ProgramService {

    private final ProgramMapper programMapper;

    /**
     * 조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회
     * @param request 조회 조건 (프로그램경로코드, 프로그램ID, 프로그램명, 사용여부)
     * @return 프로그램 목록
     */
    @Override
    public List<ProgramResponse> getProgramList(ProgramRequest request) {
        return programMapper.selectProgramList(request);
    }

    /**
     * 프로그램의 컴포넌트 목록을 정렬순번 순으로 조회
     * @param prgmId 프로그램ID
     * @return 컴포넌트 목록
     */
    @Override
    public List<ProgramComponentResponse> getProgramComponentList(String prgmId) {
        return programMapper.selectProgramComponentList(prgmId);
    }

    /**
     * 프로그램·컴포넌트 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 프로그램 삭제(소속 컴포넌트 포함) → 수정 → 등록 → 컴포넌트 삭제 → 수정 → 등록
     * @param request 프로그램·컴포넌트 변경분
     * @param usrId 저장 사용자ID
     * @return 저장 건수
     * @throws BusinessException 등록 대상이 이미 있거나 수정 대상이 없거나, 삭제 대상 프로그램을 메뉴가 사용 중인 경우
     */
    @Override
    @Transactional
    public int saveProgramList(ProgramSaveRequest request, String usrId) {
        int savedCnt = 0;

        // 1) 프로그램 삭제: 메뉴가 사용 중이면 막고, FK 때문에 컴포넌트 권한 → 컴포넌트 순으로 먼저 지운다
        for (ProgramItemRequest item : request.getDeleteList()) {
            RowSaveUtil.checkNotExists(programMapper.existsProgramMenu(item.getPrgmId()),
                    "sy.program.save.menu.exists", item.getPrgmId());
            deleteComponentAuthList(item.getPrgmId(), null);
            programMapper.deleteProgramComponentList(item.getPrgmId());
            savedCnt += programMapper.deleteProgram(item.getPrgmId());
        }
        // 2) 프로그램 수정
        for (ProgramItemRequest item : request.getUpdateList()) {
            savedCnt += RowSaveUtil.checkUpdated(programMapper.updateProgram(item, usrId));
        }
        // 3) 프로그램 등록
        for (ProgramItemRequest item : request.getInsertList()) {
            RowSaveUtil.checkNotExists(programMapper.existsProgram(item.getPrgmId()),
                    "sy.program.save.id.exists", item.getPrgmId());
            savedCnt += programMapper.insertProgram(item, usrId);
        }

        // 4) 컴포넌트: 프로그램별 삭제 → 수정 → 등록
        for (ProgramComponentSaveRequest cmpnSave : request.getCmpnSaveList()) {
            String prgmId = cmpnSave.getPrgmId();
            for (ProgramComponentItemRequest item : cmpnSave.getDeleteList()) {
                deleteComponentAuthList(prgmId, item.getCmpnId());
                savedCnt += programMapper.deleteProgramComponent(prgmId, item.getCmpnId());
            }
            for (ProgramComponentItemRequest item : cmpnSave.getUpdateList()) {
                item.setPrgmId(prgmId);
                savedCnt += RowSaveUtil.checkUpdated(programMapper.updateProgramComponent(item, usrId));
            }
            for (ProgramComponentItemRequest item : cmpnSave.getInsertList()) {
                item.setPrgmId(prgmId);
                RowSaveUtil.checkNotExists(programMapper.existsProgramComponent(prgmId, item.getCmpnId()),
                        "sy.program.save.component.id.exists", prgmId, item.getCmpnId());
                savedCnt += programMapper.insertProgramComponent(item, usrId);
            }
        }
        return savedCnt;
    }

    /**
     * 컴포넌트를 참조하는 권한그룹·사용자 컴포넌트 권한을 삭제
     * @param prgmId 프로그램ID
     * @param cmpnId 컴포넌트ID (null 이면 프로그램의 모든 컴포넌트)
     */
    private void deleteComponentAuthList(String prgmId, String cmpnId) {
        programMapper.deleteProgramAuthGroupComponentList(prgmId, cmpnId);
        programMapper.deleteProgramUserComponentList(prgmId, cmpnId);
    }
}
