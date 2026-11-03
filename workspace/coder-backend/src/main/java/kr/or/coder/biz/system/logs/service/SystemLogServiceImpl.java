package kr.or.coder.biz.system.logs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.biz.system.logs.dto.LogUsrResponse;
import kr.or.coder.biz.system.logs.dto.SystemLogPageResponse;
import kr.or.coder.biz.system.logs.dto.SystemLogRequest;
import kr.or.coder.biz.system.logs.dto.SystemLogResponse;
import kr.or.coder.biz.system.logs.mapper.SystemLogMapper;
import kr.or.coder.common.log.LogMdcKey;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SYLO030 시스템로그관리 Service 구현 (logback-spring.xml 의 에러 로그 JSON Lines 파일을 직접 읽는다)
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
@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    private static final Pattern LOG_KEY_PATTERN = Pattern.compile("^(\\d{8})-(\\d{1,4})-([1-9]\\d{0,8})$");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    /** 오래된 순 (로그일시가 없으면 가장 오래된 것으로 본다) */
    private static final Comparator<SystemLogResponse> OLDEST_FIRST =
            Comparator.comparing(SystemLogResponse::getLogDttm, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final SystemLogMapper systemLogMapper;
    private final ObjectMapper objectMapper;
    private final Path logDir;
    /** 에러 로그 파일명 앞부분 ({앱명}-error.), 파일명은 {앱명}-error.{yyyy-MM-dd}.{순번}.log */
    private final String filePrefix;
    private final Pattern fileIndexPattern;
    /** 최대 조회 기간 (일) */
    private final int maxPeriodDays;
    /** 최대 반환 건수 */
    private final int maxRows;

    /**
     * 로그 폴더·파일명은 logback-spring.xml 과 같은 규칙(LOG_PATH, 없으면 실행 폴더의 logs / spring.application.name)으로 정한다
     * @param systemLogMapper 사용자 정보 조회 Mapper
     * @param objectMapper JSON 파서
     * @param logPath 로그 폴더 (LOG_PATH)
     * @param appNm 앱명 (spring.application.name, 에러 로그 파일명 앞부분)
     * @param maxPeriodDays 최대 조회 기간 (일, coder.system-log.max-period-days)
     * @param maxRows 최대 반환 건수 (coder.system-log.max-rows)
     */
    public SystemLogServiceImpl(SystemLogMapper systemLogMapper, ObjectMapper objectMapper,
                                @Value("${LOG_PATH:logs}") String logPath,
                                @Value("${spring.application.name}") String appNm,
                                @Value("${coder.system-log.max-period-days:31}") int maxPeriodDays,
                                @Value("${coder.system-log.max-rows:1000}") int maxRows) {
        this.systemLogMapper = systemLogMapper;
        this.objectMapper = objectMapper;
        this.logDir = Paths.get(logPath).toAbsolutePath().normalize();
        this.filePrefix = appNm + "-error.";
        this.fileIndexPattern = Pattern.compile("^" + Pattern.quote(filePrefix) + "\\d{4}-\\d{2}-\\d{2}\\.(\\d{1,4})\\.log$");
        this.maxPeriodDays = maxPeriodDays;
        this.maxRows = maxRows;
    }

    /**
     * 조회 조건에 맞는 시스템로그 목록을 최근 순으로 조회 (스택트레이스 제외, 최대 건수까지)
     * @param request 조회 조건 (기간, 시스템구분, 사용자구분, 사용자ID/명)
     * @return 시스템로그 목록, 전체 건수, 잘림 여부
     * @throws BusinessException 기간이 없거나 형식이 틀렸거나 최대 기간을 넘는 경우
     */
    @Override
    public SystemLogPageResponse getSystemLogList(SystemLogRequest request) {
        LocalDate stDt = parseYmd(request.getStDt());
        LocalDate endDt = parseYmd(request.getEndDt());
        if (stDt == null || endDt == null || stDt.isAfter(endDt)) {
            throw new BusinessException("cm.validate.period.required");
        }
        if (ChronoUnit.DAYS.between(stDt, endDt) >= maxPeriodDays) {
            throw new BusinessException("cm.validate.period.max", maxPeriodDays);
        }

        // 1) 사용자명·사용자구분 조건은 조회 전에 해당 사용자ID 목록으로 바꾼다 (없으면 조건 제외)
        String usrNm = trimToNull(request.getUsrNm());
        String usrClsCd = trimToNull(request.getUsrClsCd());
        Set<String> usrIdFilter = usrNm == null && usrClsCd == null
                ? null
                : new HashSet<>(systemLogMapper.selectLogUsrIdList(usrNm, usrClsCd));

        // 2) 파일을 한 줄씩 읽어 조건에 맞는 로그 건수를 세고, 최근 maxRows 건만 남긴다
        LogCollector collector = new LogCollector(maxRows);
        if (usrIdFilter == null || !usrIdFilter.isEmpty()) {
            for (LocalDate date = stDt; !date.isAfter(endDt); date = date.plusDays(1)) {
                for (Map.Entry<Integer, Path> file : getLogFileMap(date).entrySet()) {
                    readLogFile(date, file.getKey(), file.getValue(), request, usrIdFilter, collector);
                }
            }
        }

        // 3) 남긴 로그를 최근 순으로 정렬하고 사용자 정보를 붙인다
        List<SystemLogResponse> logList = new ArrayList<>(collector.recentRows);
        logList.sort(OLDEST_FIRST.reversed());
        attachUsr(logList);
        return new SystemLogPageResponse(logList, collector.totalCnt, collector.totalCnt > maxRows);
    }

    /**
     * 로그 키로 시스템로그 1건을 스택트레이스까지 조회
     * @param logKey 로그 키 ({일자}-{파일순번}-{줄번호})
     * @return 시스템로그 상세
     * @throws BusinessException 키 형식이 틀렸거나 로그가 없는 경우
     */
    @Override
    public SystemLogResponse getSystemLog(String logKey) {
        Matcher matcher = LOG_KEY_PATTERN.matcher(logKey == null ? "" : logKey);
        LocalDate date = matcher.matches() ? parseYmd(matcher.group(1)) : null;
        if (date == null) {
            throw new BusinessException("cm.error.entity.not.found");
        }
        int fileIdx = Integer.parseInt(matcher.group(2));
        long lineNo = Long.parseLong(matcher.group(3));
        Path file = getLogFile(date, fileIdx);
        if (!Files.isRegularFile(file)) {
            throw new BusinessException("cm.error.entity.not.found");
        }

        String line;
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            line = lines.skip(lineNo - 1).findFirst().orElse(null);
        } catch (IOException | UncheckedIOException e) {
            throw new BusinessException("cm.error.entity.not.found", e);
        }
        SystemLogResponse row = line == null ? null : parseLine(line, logKey, true);
        if (row == null) {
            throw new BusinessException("cm.error.entity.not.found");
        }
        attachUsr(List.of(row));
        return row;
    }

    /**
     * 해당 일자의 에러 로그 파일을 파일순번 순으로 반환
     * @param date 일자
     * @return 파일순번 → 파일 경로 (없으면 빈 맵)
     */
    private Map<Integer, Path> getLogFileMap(LocalDate date) {
        Map<Integer, Path> fileMap = new TreeMap<>();
        if (!Files.isDirectory(logDir)) {
            return fileMap;
        }
        String glob = filePrefix + date.format(FILE_DATE) + ".*.log";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, glob)) {
            for (Path path : stream) {
                Matcher matcher = fileIndexPattern.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    fileMap.put(Integer.parseInt(matcher.group(1)), path);
                }
            }
        } catch (IOException e) {
            log.warn("시스템로그 파일 목록 조회 실패: dir={}, date={}", logDir, date, e);
        }
        return fileMap;
    }

    /**
     * 일자와 파일순번으로 에러 로그 파일 경로를 만든다
     * @param date 일자
     * @param fileIdx 파일순번
     * @return 파일 경로
     */
    private Path getLogFile(LocalDate date, int fileIdx) {
        return logDir.resolve(filePrefix + date.format(FILE_DATE) + "." + fileIdx + ".log");
    }

    /**
     * 에러 로그 파일을 한 줄씩 읽어 시스템구분·사용자ID 조건에 맞는 로그를 collector 에 넘긴다 (스택트레이스 제외)
     * @param date 파일 일자
     * @param fileIdx 파일순번
     * @param file 파일 경로
     * @param request 조회 조건
     * @param usrIdFilter 사용자명·사용자구분 조건에 맞는 사용자ID 목록 (null 이면 조건 제외)
     * @param collector 건수를 세고 최근 로그를 남기는 수집기
     */
    private void readLogFile(LocalDate date, int fileIdx, Path file, SystemLogRequest request,
                             Set<String> usrIdFilter, LogCollector collector) {
        String sysmClsCd = trimToNull(request.getSysmClsCd());
        String usrId = trimToNull(request.getUsrId());
        String keyPrefix = date.format(YMD) + "-" + fileIdx + "-";
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            long lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                SystemLogResponse row = parseLine(line, keyPrefix + lineNo, false);
                if (row == null
                        || (sysmClsCd != null && !sysmClsCd.equals(row.getSysmClsCd()))
                        || (usrId != null && (row.getUsrId() == null
                            || !row.getUsrId().toUpperCase().contains(usrId.toUpperCase())))
                        || (usrIdFilter != null && (row.getUsrId() == null || !usrIdFilter.contains(row.getUsrId())))) {
                    continue;
                }
                collector.add(row);
            }
        } catch (IOException | UncheckedIOException e) {
            log.warn("시스템로그 파일 읽기 실패: file={}", file, e);
        }
    }

    /**
     * 에러 로그 JSON 한 줄을 시스템로그로 변환 (JSON 이 아니면 null)
     * @param line JSON 한 줄
     * @param logKey 로그 키
     * @param withStackTrace 스택트레이스 포함 여부
     * @return 시스템로그 (변환 불가 시 null)
     */
    private SystemLogResponse parseLine(String line, String logKey, boolean withStackTrace) {
        if (!StringUtils.hasText(line)) {
            return null;
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(line);
        } catch (IOException e) {
            return null;
        }
        if (node == null || !node.isObject()) {
            return null;
        }
        SystemLogResponse row = new SystemLogResponse();
        row.setLogKey(logKey);
        row.setLogDttm(text(node, LogMdcKey.LOG_DTTM));
        row.setSysmClsCd(text(node, LogMdcKey.SYSM_CLS_CD));
        row.setUsrId(text(node, LogMdcKey.USR_ID));
        row.setErrMsg(text(node, LogMdcKey.ERR_MSG));
        row.setSqlId(text(node, LogMdcKey.SQL_ID));
        row.setErrCd(text(node, LogMdcKey.ERR_CD));
        row.setTraceId(text(node, LogMdcKey.TRACE_ID));
        row.setUri(text(node, LogMdcKey.URI));
        row.setMethod(text(node, LogMdcKey.METHOD));
        row.setLogger(text(node, LogMdcKey.LOGGER));
        row.setThread(text(node, LogMdcKey.THREAD));
        if (withStackTrace) {
            row.setStackTrace(text(node, LogMdcKey.STACK_TRACE));
        }
        return row;
    }

    /**
     * 로그의 사용자ID로 사용자명·사용자구분·부서를 조회해 붙인다
     * @param logList 시스템로그 목록
     */
    private void attachUsr(List<SystemLogResponse> logList) {
        Set<String> usrIdSet = logList.stream()
                .map(SystemLogResponse::getUsrId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (usrIdSet.isEmpty()) {
            return;
        }
        Map<String, LogUsrResponse> usrMap = systemLogMapper.selectLogUsrList(usrIdSet).stream()
                .collect(Collectors.toMap(LogUsrResponse::getUsrId, Function.identity(), (a, b) -> a));
        for (SystemLogResponse row : logList) {
            LogUsrResponse usr = row.getUsrId() == null ? null : usrMap.get(row.getUsrId());
            if (usr != null) {
                row.setUsrNm(usr.getUsrNm());
                row.setUsrClsCd(usr.getUsrClsCd());
                row.setDeptCd(usr.getDeptCd());
            }
        }
    }

    /**
     * JSON 필드 값을 문자열로 반환
     * @param node JSON 객체
     * @param field 필드명
     * @return 필드 값 (없으면 null)
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * YYYYMMDD 문자열을 일자로 변환
     * @param ymd YYYYMMDD
     * @return 일자 (형식이 틀리면 null)
     */
    private LocalDate parseYmd(String ymd) {
        if (ymd == null || !ymd.matches("\\d{8}")) {
            return null;
        }
        try {
            return LocalDate.parse(ymd, YMD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 앞뒤 공백을 지우고 빈 문자열이면 null 로 반환
     * @param value 입력값
     * @return 정리된 값 또는 null
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 조건에 맞는 로그 건수를 세면서 최근 maxRows 건만 남기는 수집기 (가장 오래된 로그가 맨 앞인 힙)
     */
    private static final class LogCollector {

        private final PriorityQueue<SystemLogResponse> recentRows = new PriorityQueue<>(OLDEST_FIRST);
        private final int maxRows;
        private int totalCnt;

        /**
         * 남길 최대 건수로 수집기를 생성
         * @param maxRows 남길 최대 건수
         */
        LogCollector(int maxRows) {
            this.maxRows = maxRows;
        }

        /**
         * 로그 1건을 세고, 최근 maxRows 건 안에 들면 남긴다
         * @param row 시스템로그
         */
        void add(SystemLogResponse row) {
            totalCnt++;
            if (recentRows.size() < maxRows) {
                recentRows.add(row);
            } else if (OLDEST_FIRST.compare(row, recentRows.peek()) > 0) {
                recentRows.poll();
                recentRows.add(row);
            }
        }
    }
}
