package kr.or.coder.api.common.auth;

import kr.or.coder.api.common.auth.dto.ClientKeyIssueResult;
import kr.or.coder.api.common.config.ApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 기동 인자 --coder.api.key-issue.lnk-org-id={연계기관ID} 가 있으면 클라이언트 키를 발급해 콘솔에 한 번 출력하고 종료한다.
 * 연계 관리 화면(coder-backend, Phase 3) 전까지 쓰는 발급 수단이다. 비밀키 원문은 로그 파일에 남기지 않도록 표준 출력으로만 보낸다.
 * <pre>
 * gradlew bootRun --args="--coder.api.key-issue.lnk-org-id=LOCAL01 --server.port=0"
 * </pre>
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
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "coder.api.key-issue", name = "lnk-org-id")
public class ClientKeyIssueRunner implements ApplicationRunner {

    private final ClientKeyIssueService clientKeyIssueService;
    private final ApiProperties apiProperties;
    private final ConfigurableApplicationContext context;

    /**
     * 클라이언트 키를 발급해 출력하고 애플리케이션을 종료한다
     * @param args 기동 인자
     */
    @Override
    public void run(ApplicationArguments args) {
        ApiProperties.KeyIssue keyIssue = apiProperties.getKeyIssue();
        int exitCode = 0;
        try {
            ClientKeyIssueResult result = clientKeyIssueService.issueClientKey(
                    keyIssue.getLnkOrgId(), keyIssue.getExpireDays(), apiProperties.getSystemUserId());
            System.out.println();
            System.out.println("==== coder-api client key issued (the secret is shown only once) ====");
            System.out.println("lnk_org_id    : " + keyIssue.getLnkOrgId());
            System.out.println("client_id     : " + result.keyId());
            System.out.println("client_secret : " + result.clientSecret());
            System.out.println("expires_at    : " + (result.expiDttm() == null ? "never" : result.expiDttm()));
            System.out.println("=====================================================================");
        } catch (RuntimeException e) {
            System.err.println("client key issue failed: " + e.getMessage());
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
