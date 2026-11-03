package kr.or.coder.biz.system.link.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SYLK010 기관별 허용 인터페이스 저장 요청 (전체 교체)
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
public class LinkOrgInterfaceSaveRequest {

    /** 허용할 인터페이스ID 목록 (목록에 없는 기존 허용은 사용 안 함으로 변경) */
    @NotNull
    private List<@Pattern(regexp = "^IF[A-Z]{2}[0-9]{4}$") String> infcIdList;
}
