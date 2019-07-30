/**
 *  ESP Easy DTH (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
 
import groovy.json.JsonSlurper
import groovy.transform.Field


metadata {
	definition (name: "ESP Auto Toilet", namespace: "ShinJjang", author: "ShinJjang") {
		capability "Valve"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Motion Sensor"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Sensor"
        attribute "autoMode", "enum", ["off", "on"]        
        attribute "autoBlock", "enum", ["off", "on"]        
        attribute "distance", "enum", ["off", "on"]        
        attribute "distanceLevel", "number"
        
        command "refresh"
        command "autoModeOn"
        command "autoModeOff"
        command "autoBlockOn"
        command "autoBlockOff"
        command "distanceOn"
        command "distanceOff"
        command "getStatusOfESPEasy"
	}


	simulator {
	}
    preferences {
		input "url", "text", title: "ESP IP주소", description: "Auto Toilet 로컬IP 주소를 입력", required: true
		input "ipa", "number", title: "ST IP주소1", description: "ST 로컬IP 주소 첫번째 자리", range: "100..999", required: true
		input "ipb", "number", title: "ST IP주소2", description: "ST 로컬IP 주소 두번째 자리", range: "1..999", required: true
		input "ipc", "integer", title: "ST IP주소3", description: "ST 로컬IP 주소 세번째 자리", range: "1..999", required: true
		input "ipd", "number", title: "ST IP주소4", description: "ST 로컬IP 주소 네번째 자리", range: "1..255", required: true
		input "onDistance", "number", title: "ON Distance", description: "착좌로 인식할 거리", range: "10..80", defaultValue: 50
    }

	tiles(scale: 2) {
        
        multiAttributeTile(name:"motion", type:"generic", width: 6, height: 4) {
            tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
                attributeState "active", label:'Present', icon:"st.Bath.bath5", backgroundColor:"#fc9505"
                attributeState "inactive", label:'no Present', icon:"st.Bath.bath5", backgroundColor:"#ffffff"
            }
            tileAttribute ("device.lastCheckin", key: "SECONDARY_CONTROL") {
				attributeState "lastCheckin", label:'Updated: ${currentValue}'
            }
        }       
		standardTile ("valve", "device.valve", inactiveLabel: false, width: 2, height: 2) {
				state "open", label: '${name}', action: "valve.close", icon: "st.valves.water.open", backgroundColor: "#00A0DC", nextState:"closing"
				state "closed", label: '${name}', action: "valve.open", icon: "st.valves.water.closed", backgroundColor: "#ffffff", nextState:"opening"
				state "opening", label: '${name}', action: "valve.close", icon: "st.valves.water.open", backgroundColor: "#00A0DC", nextState:"closing"
				state "closing", label: '${name}', action: "valve.open", icon: "st.valves.water.closed", backgroundColor: "#ffffff", nextState:"opening"
			}
            
        standardTile("autoMode", "device.autoMode", inactiveLabel: false, width: 2, height: 2) {
            state "on", label:'Auto ON', action:"autoModeOff", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOff"
            state "off", label:'Auto OFF', action:"autoModeOn", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOn"
             
        	state "turningOn", label:'....', action:"autoModeOff", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOff"
            state "turningOff", label:'....', action:"autoModeOn", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOn"
        }

        standardTile("distance", "device.distance", inactiveLabel: false, width: 2, height: 2) {
            state "on", label:'Distance', action:"distanceOff", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOff"
            state "off", label:'Distance', action:"distanceOn", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOn"
             
        	state "turningOn", label:'....', action:"distanceOff", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOff"
            state "turningOff", label:'....', action:"distanceOn", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOn"
        }

        valueTile("distanceLevel", "device.distanceLevel", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "distanceLevel", label:'${currentValue}'
        }
        valueTile("distanceCap", "device.distanceCap", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "distanceLevel", label:'누르면->\n30초간 작동'
        }
        standardTile("blank", "blank", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "blank", label:''
        }

       	main (["motion"])
      	details(["motion", "valve", "autoMode", "autoBlock", "blank", "distanceCap", "distance", "distanceLevel"])
	}
}


def updated() {
    log.debug "URL >> ${url}"
	state.address = url
	state.ip1 = ipa
	state.ip2 = ipb
	state.ip3 = ipc
	state.ip4 = ipd
	state.dis1 = onDistance
	//state.dis2 = offDistance
    //state.dis3 = pTime
    //state.dis4 = dTime
    setServer()
	setDistance()
//    timerLoop()
//    pollco()
}

def parse(String description) {
    def events = []

    log.debug "Parsing '${description}'"
    def msg = parseLanMessage(description)
    log.debug "headers ${msg.headers}"
    log.debug "header ${msg.header}"
//    log.debug "json ${msg.json}"
//    log.debug "status ${msg.status}"
//    log.debug "data ${msg.data}"
//    log.debug "body ${msg.body}"

    
    def desc = msg.header.toString()
	log.debug "def: descr ${desc}"

	def descr = desc.split("&")[1]
	log.debug "def: descr1 ${descr}"


    def slurper = new JsonSlurper()
    def result = slurper.parseText(descr)
    log.debug "def: descr3 ${result}"
    
    if (result.containsKey("valve")) {
       	events << createEvent(name:"valve", value: result.valve)
       	events << createEvent(name:"switch", value: (result.valve = "open" ? "on" : "off"))
    }
    if (result.containsKey("motion")) {
       	events << createEvent(name:"motion", value: result.motion)
    }
    if (result.containsKey("temperature")) {
       	events << createEvent(name:"temperature", value: result.temperature)
    }
    if (result.containsKey("autoMode")) {
       	events << createEvent(name:"autoMode", value: result.autoMode)
    }
    if (result.containsKey("sendDistance")) {
       	events << createEvent(name:"distance", value: result.sendDistance)
    }
    if (result.containsKey("distanceLevel")) {
       	events << createEvent(name:"distanceLevel", value: result.distanceLevel)
    }
    
    	def nowk = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        def now = new Date()
        state.lastTime = now.getTime()
        sendEvent(name: "lastCheckin", value: nowk)

    return events    
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg, json, status
//    log.debug "${msg}, ${json}, ${status}"
    try {
//        msg = parseLanMessage(hubResponse.description)
//        log.debug msg.body
//        def jsonObj = new JsonSlurper().parseText(msg.body)

//        def jsonObj = msg.json
//        setData(jsonObj)
//	log.debug "SetData >> ${jsonObj.Sensors}"
        
    } catch (e) {
        log.debug "Done"
    }
}


def refresh() {
}

/*def pollco() {
    def url = "https://api.thingspeak.com/update?key=${apiKey}&field${coField}=${state.carbonDioxide}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
    def refreshTime = (updatecycle as int) * 60
    	runIn(refreshTime, pollco)        
        log.debug "Update Temperature to ThingSpeak = ${state.carbonDioxide}C"
}    */

def open() {
	setValve(1)
}

def close() {
	setValve(0)
}

def on() {
	setValve(1)
}

def off() {
	setValve(0)
}

def setValve(angle) {
    try{
        def options = [
            "method": "GET",
            "path": "/control?cmd=gpio,14,${angle}",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}

def autoModeOn() {
    log.debug "autoModeOn"
	setTaskValue(3, 2, 1)
}

def autoModeOff() {
    log.debug "autoModeOff"
	setTaskValue(3, 2, 0)
}

def autoBlockOn() {
    log.debug "autoBlockOn"
	setTaskValue(3, 3, 1)
}

def autoBlockOff() {
    log.debug "autoBlockOff"
	setTaskValue(3, 3, 0)
}

def distanceOn() {
    log.debug "distanceOn"
	setTaskValue(3, 4, 1)
}

def distanceOff() {
    log.debug "distanceOff"
	setTaskValue(3, 4, 0)
}

def setServer() {
   	for (i in 1..4) {
   		def ip = state."ip${i}"
    	def setip = ip as int
		setTaskValue(4, i, setip)
    }
}

def setDistance() {
    for (i in 1..4) {
    	def dis = state."dis${i}"
    	def setdis = dis as int
		setTaskValue(5, i, setdis)
	}
}

def setTaskValue(dnum, vnum, value) {
    try{
        log.debug "setTask=${dnum}, ${vnum}, ${value}"
        def options = [
            "method": "GET",
            "path": "/control?cmd=TaskValueSet,${dnum},${vnum},${value}",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}
/*
def setServer() {
    try{
    	for (i in 1..4) {
        def ip = state."ip${i}"
        log.debug "ip=${ip}"
        def setip = ip as int
        log.debug "setip=${setip}"
        def options = [
            "method": "GET",
            "path": "/control?cmd=TaskValueSet,4,${i},${setip}",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    	}
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}


def setDistance() {
    try{
    	for (i in 2..4) {
        def dis = state."dis${i}"
        log.debug "dis=${dis}"
        def setdis = dis as int
        log.debug "setdis=${setdis}"
        def options = [
            "method": "GET",
            "path": "/control?cmd=TaskValueSet,3,${i},${setdis}",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    	}
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}
*/
/*
def setServo(angle) {
    try{
//    	def timeGap = new Date().getTime() - Long.valueOf(state.lastTime)
//        if(timeGap > 1000 * 60){
//            log.warn "ESP Easy device is not connected..."
//        }
//		log.debug "Try to get data from ${state.address}"
        def options = [
            "method": "GET",
            "path": "/control?cmd=Servo,1,13,${angle}",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}*/

/*try {
    httpPut("http://192.168.31.74/control?cmd=Servo,1,15,90") { resp ->
        log.debug "response data: ${resp.data}"
        log.debug "response contentType: ${resp.contentType}"
    }
} catch (e) {
    log.debug "something went wrong: $e"
}
/*    try{
		log.debug "Try to get data from ${state.address}"
        def options = [
            "method": "GET",
            "path": "/control?cmd=Servo,1,15,90",
            "headers": [
                "HOST": state.address + ":80",
                "Content-Type": "application/json"
            ]
        ]
        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
        sendHubCommand(myhubAction)
    }catch(e){
    	log.error "Error!!! ${e}"
    }
}*/