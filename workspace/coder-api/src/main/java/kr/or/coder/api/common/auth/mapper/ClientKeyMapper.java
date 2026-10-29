package kr.or.coder.api.common.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 클라이언트 키 발급 Mapper (TB_LNK_ORG_M 확인, TB_LNK_KEY_M 등록, TB_LNK_KEY_H 이력)
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
@Mapper
public interface ClientKeyMapper {

    /**
     * 연계기관이 등록되어 있는지 건수로 조회한다.
     * @param lnkOrgId 연계기관ID
     * @return 건수 (0 또는 1)
     */
    int selectLinkOrgCount(@Param("lnkOrgId") String lnkOrgId);

    /**
     * 클라이언트 키를 사용 상태로 등록한다.
     * @param keyId 키ID (client_id)
     * @param lnkOrgId 연계기관ID
     * @param encKeyVal KMS 로 암호화한 비밀키
     * @param kmsKeyId 암호화에 쓴 KMS KEK ID
     * @param issuDttm 발급일시
     * @param expiDttm 만료일시 (없으면 만료 없음)
     * @param regrId 등록자ID
     * @return 등록 건수
     */
    int insertClientKey(@Param("keyId") String keyId, @Param("lnkOrgId") String lnkOrgId,
                        @Param("encKeyVal") String encKeyVal, @Param("kmsKeyId") String kmsKeyId,
                        @Param("issuDttm") LocalDateTime issuDttm, @Param("expiDttm") LocalDateTime expiDttm,
                        @Param("regrId") String regrId);

    /**
     * 클라이언트 키 상태 변경 이력을 등록한다.
     * @param keyId 키ID
     * @param keyStatCd 변경 후 키상태코드
     * @param chgCont 변경 내용
     * @param kmsAuditId KMS 감사ID
     * @param regrId 등록자ID
     * @return 등록 건수
     */
    int insertClientKeyHist(@Param("keyId") String keyId, @Param("keyStatCd") String keyStatCd,
                            @Param("chgCont") String chgCont, @Param("kmsAuditId") String kmsAuditId,
                            @Param("regrId") String regrId);
}
