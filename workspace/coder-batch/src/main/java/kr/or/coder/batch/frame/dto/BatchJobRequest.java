package kr.or.coder.batch.frame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 배치직무 저장(등록/수정) 요청 DTO
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobRequest {

    /** 배치ID (BH + 업무 2자리 + 순번 3자리, 예: BHFL001) */
    @NotBlank
    @Pattern(regexp = "^BH[A-Z]{2}[0-9]{3}$", message = "배치ID는 BHFL001 형식(BH + 영문 대문자 2자리 + 숫자 3자리)입니다.")
    private String jobId;

    @NotBlank
    @Size(max = 100)
    private String jobNm;

    /** 실행클래스명: Job 빈을 정의한 클래스의 패키지 포함 이름 (예: kr.or.coder.batch.job.system.userstatusapply.UserStatusApplyJobConfig) */
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*([.][A-Za-z_$][A-Za-z0-9_$]*)+$", message = "실행클래스명은 패키지를 포함한 클래스 이름입니다.")
    private String execClassNm;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "업무모듈코드는 영문 대문자 2자리입니다.")
    private String wrkMdulCd;

    @Size(max = 100)
    private String scheExpr;

    @Pattern(regexp = "^(01|02)$", message = "미스파이어정책코드는 01 또는 02입니다.")
    private String msfrPlcyCd;

    @Pattern(regexp = "^[YN]$", message = "다중실행여부는 Y 또는 N입니다.")
    private String mltExecYn;

    @Size(max = 4000)
    private String jobParmCont;

    @NotBlank
    @Pattern(regexp = "^[YN]$", message = "사용여부는 Y 또는 N입니다.")
    private String useYn;

    @Size(max = 200)
    private String rmk;

    @Size(max = 50)
    private String reflPrgmId;

    /** 실패 알람 수신자ID 목록 (null 이면 기존 수신자 유지, 빈 목록이면 전체 삭제) */
    @Size(max = 100)
    private List<@NotBlank @Size(max = 10) String> alrmRcvrIdList;
}
