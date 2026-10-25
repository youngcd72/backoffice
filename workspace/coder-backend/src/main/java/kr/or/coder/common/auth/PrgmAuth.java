package kr.or.coder.common.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API를 호출할 수 있는 화면(프로그램ID)을 지정하는 어노테이션
 * 로그인 사용자가 지정한 프로그램 중 하나라도 화면 권한(사용자 또는 적용기간 내 권한그룹)이 있어야 호출할 수 있다.
 * 메서드에 붙이면 클래스에 붙인 값보다 우선한다. 어노테이션이 없는 API는 로그인만 확인한다.
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PrgmAuth {

    /**
     * 호출을 허용할 프로그램ID 목록
     * @return 프로그램ID 목록
     */
    String[] value();
}
