package kr.or.coder.api.biz.system.sample.service;

import kr.or.coder.api.biz.system.sample.dto.SampleResponse;
import kr.or.coder.api.biz.system.sample.mapper.SampleMapper;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플 업무 API Service 구현
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SampleServiceImpl implements SampleService {

    private final SampleMapper sampleMapper;

    /**
     * 공통코드 그룹과 사용 중인 상세코드 목록을 조회
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 공통코드 그룹과 상세코드 목록
     * @throws BusinessException 공통코드 그룹이 없는 경우 (cm.error.entity.not.found, 404)
     */
    @Override
    public SampleResponse getSample(String wrkMdulCd, String commCtgCd) {
        SampleResponse sample = sampleMapper.selectSample(wrkMdulCd, commCtgCd);
        if (sample == null) {
            throw new BusinessException("cm.error.entity.not.found");
        }
        sample.setCodeList(sampleMapper.selectSampleCodeList(wrkMdulCd, commCtgCd));
        return sample;
    }
}
