package kr.or.coder.api.biz.system.sample.service;

import kr.or.coder.api.biz.system.sample.dto.SampleResponse;

/**
 * 샘플 업무 API Service
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
public interface SampleService {

    /**
     * 공통코드 그룹과 사용 중인 상세코드 목록을 조회
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 공통코드 그룹과 상세코드 목록
     * @throws kr.or.coder.core.common.exception.BusinessException 공통코드 그룹이 없는 경우 (cm.error.entity.not.found, 404)
     */
    SampleResponse getSample(String wrkMdulCd, String commCtgCd);
}
