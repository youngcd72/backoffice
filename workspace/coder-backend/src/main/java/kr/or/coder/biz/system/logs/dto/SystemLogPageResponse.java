package kr.or.coder.biz.system.logs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * SYLO030 시스템로그 목록 조회 결과 (최대 건수까지만 담고 전체 건수와 잘림 여부를 함께 반환)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Getter
@AllArgsConstructor
public class SystemLogPageResponse {

    /** 시스템로그 목록 (최근 순) */
    private List<SystemLogResponse> list;

    /** 조건에 맞는 전체 건수 */
    private int totalCount;

    /** 최대 건수 초과로 잘렸는지 */
    private boolean truncated;
}
