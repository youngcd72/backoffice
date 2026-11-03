package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.InterfaceCallPageResponse;
import kr.or.coder.biz.system.link.dto.InterfaceCallRequest;
import kr.or.coder.biz.system.link.dto.InterfaceCallResponse;
import kr.or.coder.biz.system.link.mapper.InterfaceCallMapper;
import kr.or.coder.core.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SYLK010 호출이력 Service 구현 (TB_INFC_CALL_L, 요청일시 월 파티션)
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
@Service
public class InterfaceCallServiceImpl implements InterfaceCallService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String ENTITY_NOT_FOUND = "cm.error.entity.not.found";

    private final InterfaceCallMapper interfaceCallMapper;
    private final int maxPeriodDays;
    private final int maxRows;

    /**
     * 매퍼와 조회 한도 설정으로 생성
     * @param interfaceCallMapper 호출이력 매퍼
     * @param maxPeriodDays 최대 조회 기간 (일, coder.link.call-max-period-days)
     * @param maxRows 최대 반환 건수 (coder.link.call-max-rows)
     */
    public InterfaceCallServiceImpl(InterfaceCallMapper interfaceCallMapper,
                                    @Value("${coder.link.call-max-period-days:31}") int maxPeriodDays,
                                    @Value("${coder.link.call-max-rows:500}") int maxRows) {
        this.interfaceCallMapper = interfaceCallMapper;
        this.maxPeriodDays = maxPeriodDays;
        this.maxRows = maxRows;
    }

    /**
     * 조회 조건에 맞는 호출이력을 요청일시 최근 순으로 최대 건수까지 조회 (본문 제외)
     * @param request 조회 조건 (요청일자 범위 필수, 최대 기간 이내)
     * @return 호출이력 목록, 전체 건수, 잘림 여부
     * @throws BusinessException 기간이 없거나 형식이 틀렸거나 (cm.validate.period.required), 최대 기간을 넘는 경우 (cm.validate.period.max)
     */
    @Override
    public InterfaceCallPageResponse getInterfaceCallList(InterfaceCallRequest request) {
        LocalDate stDt = parseYmd(request.getStDt());
        LocalDate endDt = parseYmd(request.getEndDt());
        if (stDt == null || endDt == null || stDt.isAfter(endDt)) {
            throw new BusinessException("cm.validate.period.required");
        }
        if (ChronoUnit.DAYS.between(stDt, endDt) >= maxPeriodDays) {
            throw new BusinessException("cm.validate.period.max", maxPeriodDays);
        }
        int size = request.getSize() == null || request.getSize() <= 0 ? maxRows : Math.min(request.getSize(), maxRows);

        LocalDateTime stDttm = stDt.atStartOfDay();
        LocalDateTime endDttm = endDt.plusDays(1).atStartOfDay();
        int totalCount = interfaceCallMapper.selectInterfaceCallCount(request, stDttm, endDttm);
        List<InterfaceCallResponse> list = totalCount == 0
                ? List.of()
                : interfaceCallMapper.selectInterfaceCallList(request, stDttm, endDttm, size);
        return new InterfaceCallPageResponse(list, totalCount, totalCount > list.size());
    }

    /**
     * 호출이력 1건을 요청·응답 본문까지 조회 (요청일시는 밀리초까지 받아 그 1밀리초 범위로 찾는다)
     * @param callSn 호출일련번호
     * @param rqmtDttm 요청일시 (YYYY-MM-DDTHH:mm:ss.SSS)
     * @return 호출이력
     * @throws BusinessException 요청일시 형식이 틀렸거나 호출이력이 없는 경우 (cm.error.entity.not.found)
     */
    @Override
    public InterfaceCallResponse getInterfaceCall(long callSn, String rqmtDttm) {
        LocalDateTime stDttm;
        try {
            stDttm = LocalDateTime.parse(rqmtDttm).truncatedTo(ChronoUnit.MILLIS);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ENTITY_NOT_FOUND);
        }
        InterfaceCallResponse call = interfaceCallMapper.selectInterfaceCall(callSn, stDttm, stDttm.plus(1, ChronoUnit.MILLIS));
        if (call == null) {
            throw new BusinessException(ENTITY_NOT_FOUND);
        }
        return call;
    }

    /**
     * YYYYMMDD 문자열을 날짜로 변환
     * @param ymd 일자 (YYYYMMDD)
     * @return 날짜 (비었거나 형식이 틀리면 null)
     */
    private static LocalDate parseYmd(String ymd) {
        if (ymd == null || ymd.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(ymd.trim(), YMD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
