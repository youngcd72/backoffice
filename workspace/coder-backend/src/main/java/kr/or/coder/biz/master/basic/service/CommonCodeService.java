package kr.or.coder.biz.master.basic.service;

import kr.or.coder.biz.master.basic.dto.CommonCodeDetailResponse;
import kr.or.coder.biz.master.basic.dto.CommonCodeRequest;
import kr.or.coder.biz.master.basic.dto.CommonCodeResponse;
import kr.or.coder.biz.master.basic.dto.CommonCodeSaveRequest;

import java.util.List;

/**
 * MSBS010 공통코드관리 Service
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
public interface CommonCodeService {

    /**
     * 조회 조건에 맞는 공통코드 분류 목록을 업무모듈·정렬순번 순으로 조회
     * @param request 조회 조건 (업무모듈코드, 분류코드, 분류명)
     * @return 공통코드 분류 목록
     */
    List<CommonCodeResponse> getCommonCodeList(CommonCodeRequest request);

    /**
     * 공통분류의 상세코드 목록을 정렬순번 순으로 조회 (useYn=Y 이면 사용 중인 분류의 사용 중인 코드만)
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @param useYn 사용여부 조건 (Y: 화면 콤보용, 없으면 전체: 공통코드관리용)
     * @return 공통코드 상세 목록
     */
    List<CommonCodeDetailResponse> getCommonCodeDetailList(String wrkMdulCd, String commCtgCd, String useYn);

    /**
     * 공통코드 분류·코드 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 분류 삭제 → 분류 수정 → 분류 등록 → 코드 수정 → 코드 등록 (코드는 삭제하지 않는다)
     * @param request 분류 등록/수정/삭제 목록과 분류별 코드 등록/수정 목록
     * @param usrId 저장 사용자ID
     * @return 저장 건수
     * @throws kr.or.coder.core.common.exception.BusinessException 코드가 있는 분류를 삭제하거나, 등록 대상이 이미 있거나, 수정 대상이 없는 경우
     */
    int saveCommonCodeList(CommonCodeSaveRequest request, String usrId);
}
