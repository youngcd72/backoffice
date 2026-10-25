package kr.or.coder.biz.system.auths.service;

import kr.or.coder.biz.system.auths.dto.UserAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserAuthSaveRequest;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthSaveRequest;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthSaveRequest;
import kr.or.coder.biz.system.auths.mapper.UserAuthMapper;
import kr.or.coder.common.auth.AuthProperties;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU010 사용자권한관리 Service 구현
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
public class UserAuthServiceImpl implements UserAuthService {

    private final UserAuthMapper userAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    /**
     * 조회 조건에 맞는 사용자 목록을 사용자ID 순으로 조회
     * @param request 조회 조건 (사용자ID, 사용자명, 사용자구분, 사용여부)
     * @return 사용자 목록
     */
    @Override
    public List<UserAuthResponse> getUserAuthList(UserAuthRequest request) {
        return userAuthMapper.selectUserAuthList(request);
    }

    /**
     * 사용자ID가 등록되어 있는지 판별
     * @param usrId 사용자ID
     * @return 등록되어 있으면 true
     */
    @Override
    public boolean existsUser(String usrId) {
        return userAuthMapper.existsUser(usrId);
    }

    /**
     * 사용자의 화면 권한 목록을 조회
     * @param usrId 사용자ID
     * @return 화면 권한 목록
     */
    @Override
    public List<UserMenuAuthResponse> getUserMenuAuthList(String usrId) {
        return userAuthMapper.selectUserMenuAuthList(usrId);
    }

    /**
     * 사용자의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID (없으면 전체)
     * @return 컴포넌트 권한 목록
     */
    @Override
    public List<UserComponentAuthResponse> getUserComponentAuthList(String usrId, String prgmId) {
        return userAuthMapper.selectUserComponentAuthList(usrId, StringUtils.hasText(prgmId) ? prgmId : null);
    }

    /**
     * 사용자·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 사용자 삭제(권한그룹·즐겨찾기·화면·컴포넌트 권한 포함) → 사용자 수정 → 사용자 등록 → 화면 권한 삭제·등록 → 컴포넌트 권한 등록·수정
     * @param request 사용자·화면 권한·컴포넌트 권한 변경분
     * @param regrId 저장 사용자ID
     * @return 저장 건수
     * @throws BusinessException 등록 대상 사용자가 이미 있거나 수정 대상이 없는 경우
     */
    @Override
    @Transactional
    public int saveUserAuthList(UserAuthSaveRequest request, String regrId) {
        int savedCnt = 0;

        // 1) 사용자 삭제: FK 를 가진 권한그룹·즐겨찾기·컴포넌트·화면 권한·알람·배치 알람수신자를 먼저 지운다
        for (UserAuthItemRequest item : request.getDeleteList()) {
            String usrId = item.getUsrId();
            userAuthMapper.deleteUserComponentAuthList(usrId);
            userAuthMapper.deleteUserMenuAuthList(usrId);
            userAuthMapper.deleteUserAuthGroupList(usrId);
            userAuthMapper.deleteUserFavoriteList(usrId);
            userAuthMapper.deleteUserAlarmList(usrId);
            userAuthMapper.deleteUserBatchAlarmReceiverList(usrId);
            savedCnt += userAuthMapper.deleteUser(usrId);
        }
        // 2) 사용자 수정
        for (UserAuthItemRequest item : request.getUpdateList()) {
            savedCnt += RowSaveUtil.checkUpdated(userAuthMapper.updateUser(item, regrId));
        }
        // 3) 사용자 등록
        for (UserAuthItemRequest item : request.getInsertList()) {
            RowSaveUtil.checkNotExists(userAuthMapper.existsUser(item.getUsrId()),
                    "sy.user.auth.manage.validate.id.exists", item.getUsrId());
            savedCnt += userAuthMapper.insertUser(item, getInitPasswordHash(), regrId);
        }

        // 4) 화면 권한: 삭제 → 등록(이미 있으면 건너뜀), 화면 권한이 모두 없어진 프로그램의 컴포넌트 권한은 함께 삭제
        for (UserMenuAuthSaveRequest menuSave : request.getMenuAuthSaveList()) {
            String usrId = menuSave.getUsrId();
            for (UserMenuAuthItemRequest item : menuSave.getDeleteList()) {
                savedCnt += userAuthMapper.deleteUserMenuAuth(usrId, item.getMenuId(), item.getPrgmId());
            }
            for (UserMenuAuthItemRequest item : menuSave.getInsertList()) {
                item.setUsrId(usrId);
                savedCnt += userAuthMapper.insertUserMenuAuth(item, regrId);
            }
            userAuthMapper.deleteUserComponentAuthWithoutMenu(usrId);
        }

        // 5) 컴포넌트 권한: 등록·수정 모두 등록(이미 있으면 권한여부 수정)
        for (UserComponentAuthSaveRequest cmpnSave : request.getCmpnAuthSaveList()) {
            List<UserComponentAuthItemRequest> items = new ArrayList<>(cmpnSave.getInsertList());
            items.addAll(cmpnSave.getUpdateList());
            for (UserComponentAuthItemRequest item : items) {
                item.setUsrId(cmpnSave.getUsrId());
                item.setPrgmId(cmpnSave.getPrgmId());
                savedCnt += userAuthMapper.insertUserComponentAuth(item, regrId);
            }
        }
        return savedCnt;
    }

    /**
     * 사용자 비밀번호를 초기 비밀번호로 초기화 (BCrypt 해시 저장, 최초로그인여부 Y, 로그인오류건수 0, 잠금 해제)
     * @param usrId 사용자ID
     * @param regrId 처리 사용자ID
     * @throws BusinessException 사용자가 없는 경우
     */
    @Override
    @Transactional
    public void saveUserPasswordReset(String usrId, String regrId) {
        RowSaveUtil.checkUpdated(userAuthMapper.updateUserPasswordReset(usrId, getInitPasswordHash(), regrId));
    }

    /**
     * 초기 비밀번호(coder.auth.init-password)의 BCrypt 해시를 반환
     * @return 초기 비밀번호 해시
     */
    private String getInitPasswordHash() {
        return passwordEncoder.encode(authProperties.getInitPassword());
    }
}
