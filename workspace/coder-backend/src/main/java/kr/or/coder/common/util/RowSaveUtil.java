package kr.or.coder.common.util;

import kr.or.coder.core.common.exception.BusinessException;

/**
 * 그리드 행 저장(등록·수정·삭제) 공통 검사
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
public final class RowSaveUtil {

    private static final String ENTITY_NOT_FOUND = "cm.error.entity.not.found";

    private RowSaveUtil() {
    }

    /**
     * 수정·삭제 건수가 0이면 대상 없음 예외를 던지고, 아니면 건수를 그대로 반환
     * @param cnt 수정·삭제 건수
     * @return 수정·삭제 건수
     * @throws BusinessException 건수가 0인 경우 (cm.error.entity.not.found)
     * @example savedCnt += RowSaveUtil.checkUpdated(menuMapper.updateMenu(item, usrId));
     */
    public static int checkUpdated(int cnt) {
        if (cnt == 0) {
            throw new BusinessException(ENTITY_NOT_FOUND);
        }
        return cnt;
    }

    /**
     * 대상이 없으면 대상 없음 예외를 던짐
     * @param exists 대상이 있는지 여부
     * @throws BusinessException 대상이 없는 경우 (cm.error.entity.not.found)
     */
    public static void checkExists(boolean exists) {
        if (!exists) {
            throw new BusinessException(ENTITY_NOT_FOUND);
        }
    }

    /**
     * 대상이 이미 있으면 지정한 메시지로 업무 예외를 던짐 (등록 중복, 하위 데이터 존재 등)
     * @param exists 대상이 있는지 여부
     * @param messageCode 메시지 코드
     * @param args 메시지 치환 인자
     * @throws BusinessException 대상이 있는 경우
     * @example RowSaveUtil.checkNotExists(menuMapper.existsMenu(menuId), "sy.menu.save.id.exists", menuId);
     */
    public static void checkNotExists(boolean exists, String messageCode, Object... args) {
        if (exists) {
            throw new BusinessException(messageCode, args);
        }
    }
}
