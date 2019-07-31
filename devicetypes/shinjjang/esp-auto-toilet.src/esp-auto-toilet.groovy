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
		capability "Sensor"
        attribute "mode", "enum", ["auto", "manual"]        
        attribute "autoBlock", "enum", ["off", "on"]        
        attribute "distance", "enum", ["off", "on"]        
        attribute "distanceLevel", "number"
        
        command "refresh"
        command "auto"
        command "manual"
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
            
        standardTile("mode", "device.mode", inactiveLabel: false, width: 2, height: 2) {
            state "auto", label:'Auto ON', action:"manual", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOff"
            state "manual", label:'Auto OFF', action:"auto", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOn"
             
        	state "turningOn", label:'....', action:"manual", icon: "st.custom.sonos.muted", backgroundColor:"#d1cdd2", nextState:"turningOff"
            state "turningOff", label:'....', action:"auto", icon: "st.custom.sonos.unmuted", backgroundColor:"#73C1EC", nextState:"turningOn"
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
      	details(["motion", "valve", "mode", "blank", "distanceCap", "distance", "distanceLevel"])
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
    autosink()
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
       	events << createEvent(name:"switch", value: (result.valve == "open" ? "on" : "off"))
    }
    if (result.containsKey("motion")) {
       	events << createEvent(name:"motion", value: result.motion)
    }
    if (result.containsKey("autoMode")) {
       	events << createEvent(name:"mode", value: (result.autoMode == "on" ? "auto" : "manual"))
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

def auto() {
    log.debug "autoModeOn"
	setTaskValue(3, 2, 1)
}

def manual() {
    log.debug "autoModeOff"
	setTaskValue(3, 2, 0)
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
    for (i in 1..1) {
    	def dis = state."dis${i}"
    	def setdis = dis as int
		setTaskValue(5, i, setdis)
	}
}

def autosink() {
    if(device.currentValue('mode') == 'auto') {
    setTaskValue(3, 2, 1)
    log.debug "mode = auto"
    } else {
    setTaskValue(3, 2, 0)
    log.debug "mode = manual"
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
