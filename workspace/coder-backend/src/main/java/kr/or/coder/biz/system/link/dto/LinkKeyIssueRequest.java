package kr.or.coder.biz.system.link.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 클라이언트 키 발급 요청
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
@Setter
@NoArgsConstructor
public class LinkKeyIssueRequest {

    /** 유효기간 (일, 0이면 만료 없음) */
    @Min(0)
    @Max(3650)
    private int expireDays;
}
