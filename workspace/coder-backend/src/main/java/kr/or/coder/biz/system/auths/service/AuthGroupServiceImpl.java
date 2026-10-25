package kr.or.coder.biz.system.auths.service;

import kr.or.coder.biz.system.auths.dto.AuthGroupComponentItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupComponentResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupComponentSaveRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuSaveRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupSaveRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserCountResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserSaveRequest;
import kr.or.coder.biz.system.auths.mapper.AuthGroupMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU020 그룹권한관리 Service 구현
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
public class AuthGroupServiceImpl implements AuthGroupService {

    private final AuthGroupMapper authGroupMapper;

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹코드 순으로 조회
     * @param request 조회 조건 (그룹코드, 그룹명)
     * @return 권한그룹 목록
     */
    @Override
    public List<AuthGroupResponse> getAuthGroupList(AuthGroupRequest request) {
        return authGroupMapper.selectAuthGroupList(request);
    }

    /**
     * 권한그룹의 화면 권한 목록을 조회
     * @param usrGrpCd 그룹코드
     * @return 화면 권한 목록
     */
    @Override
    public List<AuthGroupMenuResponse> getAuthGroupMenuList(String usrGrpCd) {
        return authGroupMapper.selectAuthGroupMenuList(usrGrpCd);
    }

    /**
     * 권한그룹의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrGrpCd 그룹코드
     * @param prgmId 프로그램ID (없으면 전체)
     * @return 컴포넌트 권한 목록
     */
    @Override
    public List<AuthGroupComponentResponse> getAuthGroupComponentList(String usrGrpCd, String prgmId) {
        return authGroupMapper.selectAuthGroupComponentList(usrGrpCd, StringUtils.hasText(prgmId) ? prgmId : null);
    }

    /**
     * 권한그룹·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 그룹 삭제(그룹 사용자·화면·컴포넌트 권한 포함) → 그룹 수정 → 그룹 등록 → 화면 권한 삭제·등록 → 컴포넌트 권한 등록·수정
     * @param request 권한그룹·화면 권한·컴포넌트 권한 변경분
     * @param regrId 저장 사용자ID
     * @return 저장 건수
     * @throws BusinessException 등록 대상 그룹이 이미 있거나 수정 대상이 없는 경우
     */
    @Override
    @Transactional
    public int saveAuthGroupList(AuthGroupSaveRequest request, String regrId) {
        int savedCnt = 0;

        // 1) 그룹 삭제: FK 를 가진 그룹 사용자·컴포넌트·화면 권한을 먼저 지운다
        for (AuthGroupItemRequest item : request.getDeleteList()) {
            String usrGrpCd = item.getUsrGrpCd();
            authGroupMapper.deleteAuthGroupComponentList(usrGrpCd);
            authGroupMapper.deleteAuthGroupMenuList(usrGrpCd);
            authGroupMapper.deleteAuthGroupUserList(usrGrpCd);
            savedCnt += authGroupMapper.deleteAuthGroup(usrGrpCd);
        }
        // 2) 그룹 수정
        for (AuthGroupItemRequest item : request.getUpdateList()) {
            savedCnt += RowSaveUtil.checkUpdated(authGroupMapper.updateAuthGroup(item, regrId));
        }
        // 3) 그룹 등록
        for (AuthGroupItemRequest item : request.getInsertList()) {
            RowSaveUtil.checkNotExists(authGroupMapper.existsAuthGroup(item.getUsrGrpCd()),
                    "sy.auth.group.save.id.exists", item.getUsrGrpCd());
            savedCnt += authGroupMapper.insertAuthGroup(item, regrId);
        }

        // 4) 화면 권한: 삭제 → 등록(이미 있으면 건너뜀), 화면 권한이 모두 없어진 프로그램의 컴포넌트 권한은 함께 삭제
        for (AuthGroupMenuSaveRequest menuSave : request.getMenuSaveList()) {
            String usrGrpCd = menuSave.getUsrGrpCd();
            for (AuthGroupMenuItemRequest item : menuSave.getDeleteList()) {
                savedCnt += authGroupMapper.deleteAuthGroupMenu(usrGrpCd, item.getMenuId(), item.getPrgmId());
            }
            for (AuthGroupMenuItemRequest item : menuSave.getInsertList()) {
                item.setUsrGrpCd(usrGrpCd);
                savedCnt += authGroupMapper.insertAuthGroupMenu(item, regrId);
            }
            authGroupMapper.deleteAuthGroupComponentWithoutMenu(usrGrpCd);
        }

        // 5) 컴포넌트 권한: 등록·수정 모두 등록(이미 있으면 권한여부 수정)
        for (AuthGroupComponentSaveRequest cmpnSave : request.getCmpnSaveList()) {
            List<AuthGroupComponentItemRequest> items = new ArrayList<>(cmpnSave.getInsertList());
            items.addAll(cmpnSave.getUpdateList());
            for (AuthGroupComponentItemRequest item : items) {
                item.setUsrGrpCd(cmpnSave.getUsrGrpCd());
                item.setPrgmId(cmpnSave.getPrgmId());
                savedCnt += authGroupMapper.insertAuthGroupComponent(item, regrId);
            }
        }
        return savedCnt;
    }

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹별 사용자수와 함께 조회
     * @param request 조회 조건 (그룹코드, 그룹명, 사용여부)
     * @return 권한그룹·사용자수 목록
     */
    @Override
    public List<AuthGroupUserCountResponse> getAuthGroupUserCountList(AuthGroupRequest request) {
        return authGroupMapper.selectAuthGroupUserCountList(request);
    }

    /**
     * 권한그룹에 속한 사용자 목록을 조회
     * @param usrGrpCd 그룹코드
     * @return 그룹 사용자 목록
     */
    @Override
    public List<AuthGroupUserResponse> getAuthGroupUserList(String usrGrpCd) {
        return authGroupMapper.selectAuthGroupUserList(usrGrpCd);
    }

    /**
     * 권한그룹에 사용자를 추가하거나 제외 (한 트랜잭션)
     * @param usrGrpCd 그룹코드
     * @param request 추가/제외할 사용자ID 목록
     * @param regrId 저장 사용자ID
     * @return 처리 건수
     * @throws BusinessException 그룹이 없는 경우
     */
    @Override
    @Transactional
    public int saveAuthGroupUserList(String usrGrpCd, AuthGroupUserSaveRequest request, String regrId) {
        RowSaveUtil.checkExists(authGroupMapper.existsAuthGroup(usrGrpCd));
        int savedCnt = 0;
        for (String usrId : request.getDeleteList()) {
            savedCnt += authGroupMapper.deleteAuthGroupUser(usrGrpCd, usrId);
        }
        for (String usrId : request.getInsertList()) {
            savedCnt += authGroupMapper.insertAuthGroupUser(usrGrpCd, usrId, regrId);
        }
        return savedCnt;
    }
}
