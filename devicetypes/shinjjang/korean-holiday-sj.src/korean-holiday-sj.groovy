// JavaScript source code
/**
 *  SmartWeather Station For Korea
  *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Based on original DH codes by SmartThings and SeungCheol Lee(slasher)
 *   - SmartWeather Station Tile by SmartThings
 *   - AirKorea DTH by SeunCheol Lee(slasher)
 *   - Tile remake by ShinJjang
 *   - Created Icon by Onaldo
 *   - Merged dth SmartWeather Station Tile and AirKorea DTH by WooBooung
 *
 *
 */
include 'asynchttp_v1'
import groovy.xml.XmlUtil

metadata {
    definition(name: "Korean Holiday SJ", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.switch", vid: "generic-switch") {
        capability "Switch"
        capability "Polling"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Air Korea infos for WebCore
        attribute "holiday", "string"
        attribute "holidayName", "string"
        attribute "holidayDate", "number"
        attribute "lastCheckin", "string"

        command "refresh"
        command "pollAirKorea"
        command "pollWunderground"
    }

    preferences {
        input "accessKey", "text", type: "password", title: "Data.or.kr API Key", description: "www.data.go.kr에서 apikey 발급 받으세요", required: true
        input description: "한 주 중에 자신이 쉬는 날을 체크하세요", type: "paragraph", element: "paragraph", title: "한 주간 쉬는 날 체크"
        input name: "mon", type: "bool", title: "월요일", description: "월요일 쉬면 켜기"
        input name: "tues", type: "bool", title: "화요일", description: "화요일 쉬면 켜기"
        input name: "wed", type: "bool", title: "수요일", description: "수요일 쉬면 켜기"
        input name: "thurs", type: "bool", title: "목요일", description: "목요일 쉬면 켜기"
        input name: "fri", type: "bool", title: "금요일", description: "금요일 쉬면 켜기"
        input name: "sat", type: "bool", title: "토요일", description: "토요일 쉬면 켜기"
        input name: "sun", type: "bool", title: "일요일", description: "일요일 쉬면 켜기"
        input description: "휴가(방학)기간을 설정 할 수 있습니다", type: "paragraph", element: "paragraph", title: "휴가기간 설정"
        input "vaStart", "number", title: "휴가시작일", description: "날짜를 8자리로 표현 ex)'20190523'", range: "20190000..30000000"
        input "vaEnd", "number", title: "휴가종료일", description: "날짜를 8자리로 표현 ex)'20190529'", range: "20190000..30000000"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "holiday", type: "lighting", width: 6, height: 4){
            tileAttribute("device.holiday", key: "PRIMARY_CONTROL") {
                attributeState "default", label: '${currentValue}', action: "off", icon: "st.Entertainment.entertainment5", backgroundColor: "#00a0dc"
                attributeState "off", label: '휴일 아님', action: "on", icon: "st.Home.home11", backgroundColor: "#ffffff"

            }

            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("default", label: '♚ 오늘은 ${currentValue}')
            }
        }
        standardTile("switch", "device.switch", inactiveLabel: false, width: 2, height: 2) {
            state "on", label:'ON', action:"switch.off", icon:"st.Appliances.appliances17", backgroundColor:"#00a0dc"
            state "off", label:'OFF', action:"switch.on", icon:"st.Appliances.appliances17", backgroundColor:"#ffffff"             
        }
        valueTile("thismonth", "device.thismonth", decoration: "flat", width: 2, height: 1) {
            state "default", label: '${currentValue}'
        }
        valueTile("thisholiday", "device.thisholiday", decoration: "flat", width: 3, height: 3) {
            state "default", label: '${currentValue}'
        }
        valueTile("nextmonth", "device.nextmonth", decoration: "flat", width: 2, height: 1) {
            state "default", label: '${currentValue}'
        }
        valueTile("nextholiday", "device.nextholiday", decoration: "flat", width: 3, height: 3) {
            state "default", label: '${currentValue}'
        }
        valueTile("refresh", "device.refresh", decoration: "flat", width: 1, height: 1) {
            state "default", label: '', action: "refresh", icon: "st.secondary.refresh"
        }
        valueTile("refresh2", "device.refresh", decoration: "flat", width: 1, height: 1) {
            state "default", label: '', action: "refresh", icon: "st.secondary.refresh"
        }
        main(["holiday"])
        details(["holiday", "thismonth", "refresh", "nextmonth", "refresh", "thisholiday", "nextholiday"])

    }
}


def installed() {
    refresh()
}

def uninstalled() {
    unschedule()
}

def updated() {
    log.debug "updated()"
    if (mon) { state.day1 = "Mon" } else { state.day1 = null }
    if (tues) { state.day2 = "Tue" } else { state.day2 = null }
    if (wed) { state.day3 = "Wed" } else { state.day3 = null }
    if (thurs) { state.day4 = "Thu" } else { state.day4 = null }
    if (fri) { state.day5 = "Fri" } else { state.day5 = null }
    if (sat) { state.day6 = "Sat" } else { state.day6 = null }
    if (sun) { state.day7 = "Sun" } else { state.day7 = null }
    log.debug "updated()  1=${state.day1}/2=${state.day2}/3=${state.day3}/4=${state.day4}/5=${state.day5}/6=${state.day6}/7=${state.day7}/"
    refresh()
}

def refresh() {
    log.debug "refresh()"
    pollNextMonth()
    pollThisMonth()
    unschedule()

    schedule("10 0 0/2 1/1 * ? *", pollThisMonth)
    
    schedule("20 0 0/2 1/1 * ? *", pollNextMonth)
}

def configure() {
    log.debug "Configuare()"
}

def pollThisMonth() {
    state.preYear = new Date().format("yyyy", location.timeZone)
    state.preMonth = new Date().format("MM", location.timeZone)
    state.preDay = new Date().format("EEE", location.timeZone) 
    state.thisMonth = Integer.parseInt(state.preMonth)

    log.debug "This month - Year=${state.preYear}& Month=${state.thisMonth} & Day=${state.preDay}"

    def params = [
        uri: "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=${state.preYear}&solMonth=${state.preMonth}&ServiceKey=${accessKey}",
        requestContentType: 'application/xml'
    ]
    asynchttp_v1.get('xmlResultsHandler', params)
}

def xmlResultsHandler(response, data) {
    def preYM = new Date().format("yyyyMM", location.timeZone)
    def today = new Date().format("yyyyMMdd", location.timeZone)
    def preYear = new Date().format("yyyy", location.timeZone)
    def preMonth = new Date().format("MM", location.timeZone)
    def preDate = new Date().format("dd", location.timeZone)
    def todayforparse = new Date().format("yyyy-MM-dd", location.timeZone)
	def parsedDay = new Date().parse("yyyy-MM-dd", todayforparse);
	state.todayday = new java.text.SimpleDateFormat("EEE", Locale.KOREAN).format(parsedDay);
    def todayNum = Integer.parseInt(today)
    state.holiday = 0
    state.thisholi = null
    state.dayholi = null

    if (!response.hasError()) {
        def datathismonth
        try {
            datathismonth = response.xml
        } catch (e) {
            log.error "error parsing XML from response: $e"
        }
        if (datathismonth) {
            
            def precount = Integer.parseInt(datathismonth.body.totalCount.text())
            def count = precount - 1

            send(name: "thismonth", value: "${state.thisMonth}월 공휴일 정보")

            if (precount == 0) {
                send(name: "thisholiday", value: '공휴일 없음')
                state.holidayname = ""
            } else {

                for (i in 0..count) {
                    def date = datathismonth.body.items.item[i].locdate.text()
                    def name = datathismonth.body.items.item[i].dateName.text()
                    if (date && name) {
                        def bdate = Integer.parseInt(date)
                        def btoday = Integer.parseInt(today)
                        if (bdate == btoday) {
                            state.holiday = 1
                            state.holidayname = " " + name
                            log.debug "Today is ${state.holiday}"
                        } else {
                            log.debug "Today is ${state.holiday}"
                            state.holidayname = ""
                        }
                        
                        def update = Integer.parseInt(date) - (Integer.parseInt(preYM) * 100)
                        
                        if (date) {
                        	def theDate = "${state.yearapi}-${state.nextMonthapi}-${update}"
							def parsedDate = new Date().parse("yyyy-MM-dd", theDate);
							state.tishholiday = new java.text.SimpleDateFormat("EEE", Locale.KOREAN).format(parsedDate);
                        }
                        
                        if (state.thisholi == null) {
                            state.thisholi = update + "일(" + state.tishholiday + ") " + name + '\n'
                        } else {
                            state.thisholi = state.thisholi + update + "일(" + state.tishholiday + ") " + name + '\n'
                        }
                    }

                }
                log.debug "이번달 공휴일: ${state.thisholi}"
                send(name: "thisholiday", value: state.thisholi)

            }
               
                if(state.preDay){
                if(state.day1 != state.preDay){
                	if(state.day2 != state.preDay){
                    	if(state.day3 != state.preDay){
                        	if(state.day4 != state.preDay){
                            	if(state.day5 != state.preDay){
                                	if(state.day6 != state.preDay){
                                    	if(state.day7 != state.preDay){
                                        state.dayholi = 0
                                        }
                                        }
                                        }
                                        }
                                        }
                                        }
                                        }
                                       if(state.dayholi != 0) {
                                       state.dayholi = 1}
                                        }
                
                if (state.dayholi == 1) {
                    state.dayname = "쉬는날"
                } else {
                    state.dayname = ""
                }
                log.debug "쉬는날=${state.dayholi}"
                
                if (todayNum >= vaStart && todayNum <= vaEnd) {
                	state.vaday = 1
                    state.vaname = " 휴가"
                } else {
                	state.vaday = 0
                    state.vaname = ""
                }
                log.debug "휴가=${state.vaname}=${todayNum}=${vaStart}=${vaEnd}"
                
                if (state.holiday + state.dayholi + state.vaday != 0) {
                    send(name: "holiday", value: state.dayname + state.holidayname + state.vaname)
                    send(name: "switch", value: 'on')
                } else {
                    send(name: "holiday", value: 'off')
                    send(name: "switch", value: 'off')
                }
                    def nowT = new Date().format("HH:mm", location.timeZone)

                    send(name: "lastCheckin", value: preYear + "년 " + state.thisMonth + "월 " + preDate + "일(" + state.todayday + ")  update is " +  nowT)
                            
        }
    } else {
        log.error "error making request: ${response.getErrorMessage()}"
    }
}


def pollNextMonth() {
    def preYear = new Date().format("yyyy", location.timeZone)
    def preMonth = new Date().format("MM", location.timeZone)
    state.nextMonth = Integer.parseInt(preMonth) + 1
    state.yearapi = Integer.parseInt(preYear)
    if (state.nextMonth < 10) {
        def nextMon = state.nextMonth as String
        state.nextMonthapi = "0" + nextMon
    } else if (state.nextMonth == 13) {
        state.yearapi = state.yearapi + 1
        state.nextMonthapi = "01"
    } else {
        state.nextMonthapi = state.nextMonth
    }
    log.debug "Next Month - Year=${state.yearapi}& Month=${state.nextMonthapi}"

    def params = [
        uri: "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=${state.yearapi}&solMonth=${state.nextMonthapi}&ServiceKey=${accessKey}",
        requestContentType: 'application/xml'
    ]
    asynchttp_v1.get('xmlResultsHandler2', params)
}

def xmlResultsHandler2(response, data) {

    def preYM = new Date().format("yyyyMM", location.timeZone)
    state.nextholi = null

    if (!response.hasError()) {
        def datanextmonth
        try {
            datanextmonth = response.xml
        } catch (e) {
            log.error "error parsing XML from response: $e"
        }
        if (datanextmonth) {
            def today = new Date().format("yyyyMMdd", location.timeZone)
            def precount = Integer.parseInt(datanextmonth.body.totalCount.text())
            def count = precount - 1

            send(name: "nextmonth", value: "${state.nextMonth}월 공휴일 정보")

            if (precount == 0) {
                send(name: "nextholiday", value: '공휴일 없음')
            } else {

                for (i in 0..count) {
                    def date = datanextmonth.body.items.item[i].locdate.text()
                    def name = datanextmonth.body.items.item[i].dateName.text()
                    if (date && name) {
                        def update = Integer.parseInt(date) - (Integer.parseInt(preYM) * 100 + 100)
                        
                        if (date) {
                        	def theDate = "${state.yearapi}-${state.nextMonthapi}-${update}"
							def parsedDate = new Date().parse("yyyy-MM-dd", theDate);
							state.nextholiday = new java.text.SimpleDateFormat("EEE", Locale.KOREAN).format(parsedDate);
                        }
                        
                        if (state.nextholi == null) {
                            state.nextholi = update + "일(" + state.nextholiday + ") " + name + '\n'
                        } else {
                            state.nextholi = state.nextholi + update + "일(" + state.nextholiday + ") " + name + '\n'
                        }
                    }
                }
                log.debug "다음달 공휴일: ${state.nextholi}"
                send(name: "nextholiday", value: state.nextholi)
            }
        }
    } else {
        log.error "error making request: ${response.getErrorMessage()}"
    }
}



private send(map) {
    log.debug "K.Holiday - event: $map"
    sendEvent(map)
}