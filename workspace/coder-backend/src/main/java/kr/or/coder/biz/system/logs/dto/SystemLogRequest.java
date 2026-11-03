package kr.or.coder.biz.system.logs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLO030 시스템로그 목록 조회 조건
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
@Setter
@NoArgsConstructor
public class SystemLogRequest {

    /** 조회 시작일자 (YYYYMMDD) */
    private String stDt;

    /** 조회 종료일자 (YYYYMMDD) */
    private String endDt;

    /** 시스템구분코드 (SY003) */
    private String sysmClsCd;

    /** 사용자구분코드 (SY002) */
    private String usrClsCd;

    /** 사용자ID (부분 일치) */
    private String usrId;

    /** 사용자명 (부분 일치) */
    private String usrNm;
}
