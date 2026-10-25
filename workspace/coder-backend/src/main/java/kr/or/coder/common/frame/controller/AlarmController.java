package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.dto.alarm.AlarmConfirmRequest;
import kr.or.coder.common.frame.dto.alarm.AlarmRequest;
import kr.or.coder.common.frame.dto.alarm.AlarmResponse;
import kr.or.coder.common.frame.service.AlarmService;
import kr.or.coder.common.frame.service.AlarmServiceImpl;
import kr.or.coder.common.sse.SseEmitterRegistry;
import kr.or.coder.core.common.exception.BusinessException;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 상단 알림 센터 Controller (SSE 구독, 알람 목록·건수 조회, 확인 처리, 관리자 발송)
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Alarms", description = "CODER Alarm Center APIs (SSE)")
public class AlarmController {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final AlarmService alarmService;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 로그인 사용자의 알람 SSE 연결을 생성 (연결 직후 count 이벤트로 미확인 건수를 보낸다)
     * @param authentication 인증 정보 (사용자ID)
     * @param response 프록시 버퍼링·캐시를 끄는 헤더 설정용 응답
     * @return SSE 연결
     */
    @Operation(summary = "Subscribe Alarm Stream", description = "Opens a text/event-stream (events: count, alarm)")
    @GetMapping(value = "/alarms/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAlarmStream(Authentication authentication, HttpServletResponse response) {
        String usrId = authentication.getName();
        int unconfirmedCnt = alarmService.getUnconfirmedAlarmCount(usrId);

        response.setHeader("Cache-Control", "no-cache");
        // nginx 가 응답을 모았다가 보내지 않도록 한다
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = sseEmitterRegistry.subscribe(usrId);
        sseEmitterRegistry.send(usrId, emitter, AlarmServiceImpl.EVENT_COUNT, unconfirmedCnt);
        return emitter;
    }

    /**
     * 로그인 사용자의 최근 알람 목록을 조회
     * @param authentication 인증 정보 (사용자ID)
     * @return 알람 목록 (최근 발송 순, 최대 50건)
     */
    @Operation(summary = "Get My Alarms", description = "Returns recent alarms of the signed-in user")
    @GetMapping("/alarms")
    public ApiResponse<List<AlarmResponse>> getAlarmList(Authentication authentication) {
        return ApiResponse.success(alarmService.getAlarmList(authentication.getName()));
    }

    /**
     * 로그인 사용자의 확인하지 않은 알람 건수를 조회
     * @param authentication 인증 정보 (사용자ID)
     * @return 미확인 알람 건수
     */
    @Operation(summary = "Get Unconfirmed Alarm Count")
    @GetMapping("/alarms/unconfirmed-count")
    public ApiResponse<Integer> getUnconfirmedAlarmCount(Authentication authentication) {
        return ApiResponse.success(alarmService.getUnconfirmedAlarmCount(authentication.getName()));
    }

    /**
     * 로그인 사용자의 알람 1건을 확인 처리
     * @param alrmSn 알람일련번호
     * @param request 확인 요청 (cfmYn=Y)
     * @param authentication 인증 정보 (사용자ID)
     * @return 수정 건수 (이미 확인했거나 본인 알람이 아니면 0)
     */
    @Operation(summary = "Confirm Alarm")
    @PatchMapping("/alarms/{alrmSn}")
    public ApiResponse<Integer> saveAlarmConfirm(@PathVariable Long alrmSn,
                                                 @Valid @RequestBody AlarmConfirmRequest request,
                                                 Authentication authentication) {
        return ApiResponse.success(alarmService.saveAlarmConfirm(authentication.getName(), alrmSn));
    }

    /**
     * 로그인 사용자의 미확인 알람 전체를 확인 처리
     * @param request 확인 요청 (cfmYn=Y)
     * @param authentication 인증 정보 (사용자ID)
     * @return 수정 건수
     */
    @Operation(summary = "Confirm All Alarms")
    @PatchMapping("/alarms")
    public ApiResponse<Integer> saveAlarmConfirmAll(@Valid @RequestBody AlarmConfirmRequest request,
                                                    Authentication authentication) {
        return ApiResponse.success(alarmService.saveAlarmConfirm(authentication.getName(), null));
    }

    /**
     * 지정한 수신자에게 알람을 발송 (관리자 전용, 업무 코드에서는 AlarmService.saveAlarm 을 직접 호출)
     * @param request 알람 발송 요청
     * @param authentication 인증 정보 (발송자ID, 권한)
     * @return 등록 건수
     * @throws BusinessException 관리자가 아니거나 없는 수신자가 있는 경우
     */
    @Operation(summary = "Send Alarm (ADMIN)")
    @PostMapping("/alarms")
    public ApiResponse<Integer> saveAlarm(@Valid @RequestBody AlarmRequest request, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        if (!isAdmin) {
            throw new BusinessException("cm.error.forbidden");
        }
        return ApiResponse.success(alarmService.saveAlarm(request, authentication.getName()));
    }
}
