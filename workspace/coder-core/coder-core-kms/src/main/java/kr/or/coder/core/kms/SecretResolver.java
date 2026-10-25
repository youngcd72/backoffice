package kr.or.coder.core.kms;

/**
 * 참조키(비밀 저장소 경로)로 비밀값을 조회한다. DB·설정에는 비밀값 대신 참조키만 둔다.
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
public interface SecretResolver {

    /**
     * 참조키에 해당하는 비밀값을 조회한다.
     * @param reference 비밀 참조키
     * @return 비밀값
     * @throws IllegalStateException 참조키에 해당하는 비밀값이 없을 때
     */
    String resolve(String reference);
}
