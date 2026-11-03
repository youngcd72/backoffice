package kr.or.coder.biz.system.link.mapper;

import kr.or.coder.biz.system.link.dto.LinkKeyHistResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyResponse;
import kr.or.coder.biz.system.link.dto.LinkKeySaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SYLK010 클라이언트 키·키 이력 MyBatis Mapper (TB_LNK_KEY_M, TB_LNK_KEY_H)
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
@Mapper
public interface LinkKeyMapper {

    /**
     * 연계기관의 클라이언트 키 목록을 상태·발급일시 최근 순으로 조회 (암호화 비밀키 제외)
     * @param lnkOrgId 연계기관ID
     * @return 클라이언트 키 목록
     */
    List<LinkKeyResponse> selectLinkKeyList(@Param("lnkOrgId") String lnkOrgId);

    /**
     * 연계기관의 클라이언트 키 1건을 조회 (암호화 비밀키 제외)
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @return 클라이언트 키 (없으면 null)
     */
    LinkKeyResponse selectLinkKey(@Param("lnkOrgId") String lnkOrgId, @Param("keyId") String keyId);

    /**
     * 클라이언트 키를 사용 상태로 등록
     * @param keyId 키ID
     * @param lnkOrgId 연계기관ID
     * @param encKeyVal KMS 로 암호화한 비밀키
     * @param kmsKeyId 암호화에 쓴 KEK ID
     * @param issuDttm 발급일시
     * @param expiDttm 만료일시 (없으면 만료 없음)
     * @param usrId 등록 사용자ID
     * @return 등록 건수
     */
    int insertLinkKey(@Param("keyId") String keyId, @Param("lnkOrgId") String lnkOrgId,
                      @Param("encKeyVal") String encKeyVal, @Param("kmsKeyId") String kmsKeyId,
                      @Param("issuDttm") LocalDateTime issuDttm, @Param("expiDttm") LocalDateTime expiDttm,
                      @Param("usrId") String usrId);

    /**
     * 클라이언트 키의 상태·만료일시·허용IP·호출 한도·비고를 수정 (폐기된 키는 수정하지 않음)
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @param item 수정 내용 (만료일자는 그날 23:59:59 로 저장)
     * @param usrId 수정 사용자ID
     * @return 수정 건수
     */
    int updateLinkKey(@Param("lnkOrgId") String lnkOrgId, @Param("keyId") String keyId,
                      @Param("item") LinkKeySaveRequest item, @Param("usrId") String usrId);

    /**
     * 클라이언트 키 이력을 등록
     * @param keyId 키ID
     * @param keyStatCd 변경 후 키상태코드
     * @param chgCont 변경내용
     * @param kmsAuditId KMS 감사ID (없으면 null)
     * @param usrId 등록 사용자ID
     * @return 등록 건수
     */
    int insertLinkKeyHist(@Param("keyId") String keyId, @Param("keyStatCd") String keyStatCd,
                          @Param("chgCont") String chgCont, @Param("kmsAuditId") String kmsAuditId,
                          @Param("usrId") String usrId);

    /**
     * 클라이언트 키 이력을 최근 순으로 조회
     * @param keyId 키ID
     * @return 키 이력 목록
     */
    List<LinkKeyHistResponse> selectLinkKeyHistList(@Param("keyId") String keyId);
}
