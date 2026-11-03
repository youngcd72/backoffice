package kr.or.coder.biz.system.link.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * SYLK010 호출이력 조회 결과
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
@Getter
@AllArgsConstructor
public class InterfaceCallPageResponse {

    /** 호출이력 목록 (요청일시 최근 순) */
    private List<InterfaceCallResponse> list;

    /** 조건에 맞는 전체 건수 */
    private int totalCount;

    /** 최대 건수 초과로 잘렸는지 */
    private boolean truncated;
}
