package kr.or.coder.common.sse;

/**
 * 인스턴스 간 SSE 전송 메시지 (Redis 채널로 발행해 모든 backend 인스턴스가 받는다)
 * @param usrId 수신 사용자ID (이 사용자의 접속 중인 emitter 에만 전송)
 * @param eventName SSE 이벤트명 (alarm, count 등)
 * @param data 이벤트 데이터 (JSON 으로 직렬화)
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
public record SseMessage(String usrId, String eventName, Object data) {
}
