/**
 *  Xiaomi Air Monitor (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
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

@Field 
LANGUAGE_MAP = [
]

metadata {
	definition (name: "Xiaomi Air Detector", namespace: "fison67", author: "fison67", mnmn: "SmartThings", vid: "generic-switch") {
        capability "Air Quality Sensor"						//"on", "off"
	capability "Relative Humidity Measurement"
	capability "Temperature Measurement"
	capability "Tvoc Measurement"
        capability "Carbon Dioxide Measurement"
        capability "Refresh"
	capability "Refresh"
	capability "Sensor"
	capability "Power Source"
	capability "Dust Sensor" // fineDustLevel : PM 2.5   dustLevel : PM 10

        
        attribute "lastCheckin", "Date"
     
        command "XiaomiPM25"
        command "noAQS"
        command "noSwitch"
        command "clockOn"
        command "clockOff"
        command "nightOn"
        command "nightOff"
        command "setBeHour"
        command "setEndHour"
        command "setBeMin"
        command "setEndMin"
        command "setBePm"
        command "setBeAm"
        command "setEndPm"
        command "setEndAm"
        command "setUpTime"
        
	}


	simulator {
	}
	preferences {
	}

	tiles {
		multiAttributeTile(name:"fineDustLevel", type: "generic", width: 3, height: 2){
			tileAttribute ("device.fineDustLevel", key: "PRIMARY_CONTROL") {
                attributeState "default", label:'${currentValue}㎍/㎥', unit:"㎍/㎥", backgroundColors:[
			[value: -1, color: "#C4BBB5"],
            		[value: 0, color: "#7EC6EE"],
            		[value: 15, color: "#51B2E8"],
            		[value: 50, color: "#e5c757"],
            		[value: 75, color: "#E40000"],
            		[value: 500, color: "#970203"]
            		]
			}
            
            tileAttribute("device.battery", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Battery: ${currentValue}%\n')
            }		
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
	}
		valueTile("tvoc", "device.tvocLevel", decoration: "flat", width: 2, height: 2) {
        		state "default", label:'${currentValue}㎍/㎥', unit:"㎍/㎥", backgroundColors:[
				[value: 0.3, color: "#18cdff"],
            			[value: 1, color: "#19ffeb"],
            			[value: 3, color: "#ddf927"],
            			[value: 9, color: "#ffb71e"],
            			[value: 100, color: "#f94d1d"]
            		]
        	}
 		valueTile("carbonDioxide", "device.carbonDioxide", width: 2, height: 2, inactiveLabel: false) {
 			state "carbonDioxide", label:'${currentValue}ppm', unit:"CO2", backgroundColors: [
 				[value: 600, color: "#18cdff"],
                		[value: 999, color: "#19ffeb"],
                		[value: 1500, color: "#ddf927"],
                		[value: 2000, color: "#ffb71e"],
                		[value: 6000, color: "#f94d1d"],
 				]
 		}
		valueTile("temperature", "device.temperature", width: 2, height: 2, inactiveLabel: false) {
 			state("temperature", label: '${currentValue}°', backgroundColors: [
 				[value: 31, color: "#153591"],
 				[value: 44, color: "#1e9cbb"],
 				[value: 59, color: "#90d2a7"],
 				[value: 74, color: "#44b621"],
 				[value: 84, color: "#f1d801"],
 				[value: 95, color: "#d04e00"],
 				[value: 96, color: "#bc2323"]
 				]
 				)
 		}        
		valueTile("humidity", "device.humidity", width: 2, height: 2, inactiveLabel: false) {
 			state("humidity", label: '${currentValue}%', backgroundColors: [
 				[value: 20, color: "#f94d1d"],
 				[value: 40, color: "#ffb71e"],
 				[value: 60, color: "#ddf927"],
 				[value: 80, color: "#19ffeb"],
 				[value: 100, color: "#18cdff"]
 				]
 				)
 		}        
        	valueTile("display_label", "device.display_label", decoration: "flat") {
            		state "default", label:'${currentValue}'
        	}        
        	valueTile("night_label", "device.night_label", decoration: "flat") {
            		state "default", label:'${currentValue}'
        	}
        	valueTile("power_label", "device.power_label", decoration: "flat") {
            		state "default", label:'${currentValue}'
        	}
        	valueTile("refresh_label", "device.refresh_label", decoration: "flat") {
            		state "default", label:'${currentValue}'
        	}
        valueTile("timeset_label", "device.timeset_label", decoration: "flat", width: 1, height: 2) {
            state "default", label:'${currentValue}', action:"setUpTime"
        }
        
        valueTile("battery", "device.battery", width: 2, height: 2) {
            state("val", label:'${currentValue}%', defaultState: true, backgroundColor:"#00a0dc")
        }
	standardTile("refresh", "device.thermostatMode") {
		state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
	}
        

        
        main (["fineDustLevel"])
		details(["fineDustLevel", "tvoc", "carbonDioxide", "temperature", "humidity", "refresh"])
		
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
    log.debug "${params.key} : ${params.data}"
 
 	switch(params.key){
    case "pm2.5":
    	sendEvent(name:"fineDustLevel", value: params.data)
    	break;
    case "temperature":
    	sendEvent(name:"temperature", value: params.data)
    	break;
    case "tvoc":
    	sendEvent(name:"tvocLevel", value: params.data)
    	break;
    case "relativeHumidity":
    	sendEvent(name:"humidity", value: params.data)
    	break;
    case "co2e":
    	sendEvent(name:"carbonDioxide", value: params.data)
    	break;
    case "battery":
    	sendEvent(name:"battery", value: params.data)
        break;
    }
    
    updateLastTime()
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}


def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
		state.BeginTime = jsonObj.state.nightBeginTime
		state.EndTime = jsonObj.state.nightEndTime
		sendEvent(name:"switch", value: (jsonObj.state.power == true ? "on" : "off") )
		sendEvent(name:"fineDustLevel", value: jsonObj.state.aqi )
		sendEvent(name:"powerSource", value: (jsonObj.state.charging == true ? "dc" : "battery") )
		sendEvent(name:"battery", value: jsonObj.state.batteryLevel )
		sendEvent(name:"powerSource", value: (jsonObj.state.charging == true ? "dc" : "battery") )
		sendEvent(name:"clock", value: jsonObj.state.timeState )
		sendEvent(name:"night", value: jsonObj.state.nightState )
		updateLastTime()
        beginTime()
        endTime()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
    refresh()
    setLanguage(settings.selectedLang)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
