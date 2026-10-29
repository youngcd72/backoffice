package kr.or.coder.api.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 업무 API가 공개하는 인터페이스ID(TB_INFC_M.INFC_ID)를 선언한다.
 * 인터페이스 권한(토큰 scope, TB_LNK_ORG_INFC_R), 호출 한도, 호출 이력의 기준이 된다.
 * 업무 API 패키지(coder.api.biz-base-package)의 Controller 메서드에 선언이 없으면 기동하지 않는다.
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
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ApiInterface {

    /**
     * 인터페이스ID (IF + 업무약어 2자리 + 순번 4자리, 예: IFSY0001)
     * @return 인터페이스ID
     */
    String value();
}
