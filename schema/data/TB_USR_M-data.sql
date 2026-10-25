-- =====================================================================
-- 사용자 초기 비밀번호 설정 (TB_USR_M.PWD)
-- 비밀번호가 없는 사용자에게 초기 비밀번호 coder1234 의 BCrypt 해시를 넣는다 (로그인은 PWD 해시로 검증).
-- 운영처럼 초기 비밀번호(coder.auth.init-password)를 바꾼 환경에서는 실행하지 말고 화면의 비밀번호 초기화를 쓴다.
-- 재실행해도 비밀번호가 있는 사용자는 건드리지 않는다.
-- =====================================================================

UPDATE TB_USR_M
   SET PWD          = '$2a$10$np4CsHLCLxIsQjcaVlRaUOt/WADitW2fkwID0Qq7HPiwc4v8AoCye',
       FIRS_LGIN_YN = 'Y',
       LGIN_ERR_CNT = 0
 WHERE PWD IS NULL;
