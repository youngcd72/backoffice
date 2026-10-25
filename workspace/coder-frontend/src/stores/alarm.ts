/*****************************************************************************************
	Program ID   : alarm
	Program Type : ts
	Program Name : 알림 센터 스토어
	Description  : SSE(/api/v1/alarms/stream) 연결을 유지하며 미확인 알람 건수와 알람 목록을 관리하는 Pinia 스토어
	               - 브라우저 EventSource 는 Authorization 헤더를 보낼 수 없어 fetch 기반 SSE(@microsoft/fetch-event-source) 사용
	               - 서버 이벤트: count(미확인 건수, 연결 직후·확인 처리 후), alarm(새 알람 1건)
	               - 연결이 끊기면 3초부터 최대 60초까지 간격을 늘려 다시 연결, 401 이면 로그인 화면으로 이동
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-17  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchEventSource, EventStreamContentType } from '@microsoft/fetch-event-source'
import {
  ALARM_STREAM_URL,
  getAlarmListApi,
  saveAlarmConfirmAllApi,
  saveAlarmConfirmApi,
  type Alarm,
} from '@/api/common/alarm'
import { useToast } from '@/composables/useToast'

const TOKEN_KEY = 'coder_token'
const USER_KEY = 'coder_user'

/** 알림 센터 목록 최대 보관 건수 (backend 조회 건수와 동일) */
const MAX_ALARM_ROWS = 50

/** 재연결 대기 시간(ms): 처음 값에서 두 배씩 늘려 최대값까지 */
const RETRY_DELAY_MIN = 3000
const RETRY_DELAY_MAX = 60000

/** 다시 연결하지 않고 끝내야 하는 오류 (401, 403 등) */
class FatalStreamError extends Error {
  status: number

  constructor(status: number) {
    super(`alarm stream closed with status ${status}`)
    this.status = status
  }
}

/** 서버가 연결을 정상 종료한 경우 (연결 유지 시간 경과) — 다시 연결한다 */
class StreamClosedError extends Error {}

/**
 * useAlarmStore : 알림 센터 Pinia 스토어 정의: SSE 연결, 미확인 건수, 알람 목록, 확인 처리 제공
 * @return  N/A
 */
export const useAlarmStore = defineStore('alarm', () => {
  const toast = useToast()

  const unconfirmedCnt = ref(0)
  const alarmList = ref<Alarm[]>([])
  const isListLoaded = ref(false)
  const isListLoading = ref(false)
  const isConnected = ref(false)

  let abortController: AbortController | null = null
  let retryDelay = RETRY_DELAY_MIN

  /**
   * connect : 알람 SSE 연결을 시작 (이미 연결 중이면 무시, 토큰이 없으면 연결하지 않음)
   * @return  N/A
   */
  function connect() {
    if (abortController) return
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) return

    const controller = new AbortController()
    abortController = controller
    retryDelay = RETRY_DELAY_MIN

    fetchEventSource(`${import.meta.env.VITE_API_BASE_URL || ''}${ALARM_STREAM_URL}`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: controller.signal,
      // 탭이 백그라운드여도 연결을 유지 (기본값은 숨길 때 끊고 보일 때 다시 연결)
      openWhenHidden: true,

      async onopen(response) {
        const contentType = response.headers.get('content-type') ?? ''
        if (response.ok && contentType.startsWith(EventStreamContentType)) {
          isConnected.value = true
          retryDelay = RETRY_DELAY_MIN
          return
        }
        if (response.status === 401 || response.status === 403) {
          throw new FatalStreamError(response.status)
        }
        // 5xx, 프록시 오류 등은 재연결
        throw new Error(`alarm stream open failed: ${response.status}`)
      },

      onmessage(event) {
        if (!event.data) return
        if (event.event === 'count') {
          unconfirmedCnt.value = Number(JSON.parse(event.data)) || 0
        } else if (event.event === 'alarm') {
          receiveAlarm(JSON.parse(event.data) as Alarm)
        }
      },

      onclose() {
        isConnected.value = false
        throw new StreamClosedError()
      },

      onerror(error) {
        isConnected.value = false
        if (error instanceof FatalStreamError) throw error
        const delay = retryDelay
        retryDelay = Math.min(retryDelay * 2, RETRY_DELAY_MAX)
        return delay
      },
    }).catch((error) => {
      isConnected.value = false
      if (abortController === controller) abortController = null
      if (error instanceof FatalStreamError && error.status === 401) {
        // api/client.ts 401 처리와 동일: 세션 정리 후 로그인 화면으로 이동
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        if (window.location.pathname !== '/login') window.location.href = '/login'
        return
      }
      console.warn('[CODER Alarm] 알림 연결을 종료합니다.', error)
    })
  }

  /**
   * disconnect : 알람 SSE 연결을 끊고 상태를 초기화 (로그아웃, 레이아웃 해제 시)
   * @return  N/A
   */
  function disconnect() {
    abortController?.abort()
    abortController = null
    isConnected.value = false
    unconfirmedCnt.value = 0
    alarmList.value = []
    isListLoaded.value = false
  }

  /**
   * receiveAlarm : SSE 로 받은 새 알람을 목록 앞에 추가하고 건수를 늘린 뒤 토스트로 안내
   * @param   alarm - 받은 알람
   * @return  N/A
   */
  function receiveAlarm(alarm: Alarm) {
    unconfirmedCnt.value += 1
    if (isListLoaded.value) {
      alarmList.value = [alarm, ...alarmList.value.filter((a) => a.alrmSn !== alarm.alrmSn)].slice(0, MAX_ALARM_ROWS)
    }
    toast.info(alarm.alrmCont ? { title: alarm.alrmTitlNm, message: alarm.alrmCont } : alarm.alrmTitlNm)
  }

  /**
   * fetchAlarmList : 알람 목록을 서버에서 다시 조회 (알림 센터를 열 때)
   * @return  N/A (실패 시 Error 발생)
   */
  async function fetchAlarmList() {
    isListLoading.value = true
    try {
      alarmList.value = await getAlarmListApi()
      isListLoaded.value = true
    } finally {
      isListLoading.value = false
    }
  }

  /**
   * confirmAlarm : 알람 1건을 확인(읽음) 처리 (이미 확인한 알람은 무시)
   * @param   alarm - 확인할 알람
   * @return  N/A (실패 시 Error 발생)
   */
  async function confirmAlarm(alarm: Alarm) {
    if (alarm.cfmYn === 'Y') return
    const updatedCnt = await saveAlarmConfirmApi(alarm.alrmSn)
    alarm.cfmYn = 'Y'
    // 정확한 건수는 서버가 count 이벤트로 다시 보낸다
    if (updatedCnt > 0) unconfirmedCnt.value = Math.max(0, unconfirmedCnt.value - 1)
  }

  /**
   * confirmAllAlarms : 미확인 알람 전체를 확인(읽음) 처리
   * @return  N/A (실패 시 Error 발생)
   */
  async function confirmAllAlarms() {
    await saveAlarmConfirmAllApi()
    alarmList.value.forEach((alarm) => {
      alarm.cfmYn = 'Y'
    })
    unconfirmedCnt.value = 0
  }

  return {
    unconfirmedCnt,
    alarmList,
    isListLoaded,
    isListLoading,
    isConnected,
    connect,
    disconnect,
    fetchAlarmList,
    confirmAlarm,
    confirmAllAlarms,
  }
})
