package kr.or.coder.batch.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 배치 업무 오류코드 (HTTP 상태와 다국어 메시지 키 매핑)
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
@RequiredArgsConstructor
public enum BatchErrorCode {

    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "sy.batch.job.not.found"),
    JOB_BEAN_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "sy.batch.job.bean.not.registered"),
    JOB_CLASS_AMBIGUOUS(HttpStatus.BAD_REQUEST, "sy.batch.job.class.ambiguous"),
    JOB_ID_MISMATCH(HttpStatus.BAD_REQUEST, "sy.batch.job.id.mismatch"),
    JOB_DUPLICATE(HttpStatus.CONFLICT, "sy.batch.job.duplicate"),
    JOB_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "sy.batch.job.parameter.invalid"),
    JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "sy.batch.job.already.running"),
    ALARM_RECEIVER_NOT_FOUND(HttpStatus.BAD_REQUEST, "sy.batch.alarm.receiver.not.found"),
    SCHEDULE_INVALID_EXPRESSION(HttpStatus.BAD_REQUEST, "sy.batch.schedule.invalid.expression"),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "sy.batch.schedule.not.found"),
    SCHEDULE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "sy.batch.schedule.status.invalid"),
    SCHEDULER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "sy.batch.scheduler.error"),
    EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "sy.batch.execution.not.found"),
    EXECUTION_NOT_RESTARTABLE(HttpStatus.CONFLICT, "sy.batch.execution.not.restartable"),
    EXECUTION_NOT_STOPPABLE(HttpStatus.CONFLICT, "sy.batch.execution.not.stoppable"),
    EXECUTION_STILL_RUNNING(HttpStatus.CONFLICT, "sy.batch.execution.still.running"),
    EXECUTION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "sy.batch.execution.status.invalid");

    private final HttpStatus status;
    private final String messageKey;
}
