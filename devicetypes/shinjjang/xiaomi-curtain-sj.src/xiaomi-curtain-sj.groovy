/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  2017 
 */

metadata {
    definition(name: "Xiaomi Curtain SJ", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.light", vid: "generic-dimmer", mnmn: "SmartThings") {
		capability "Switch Level"
		capability "Actuator"
		capability "Health Check"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Light"
        capability "Configuration"
        capability "Window Shade" 

        

        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013, 0006, 0001, 0406", outClusters: "0019, 000A, 000D, 0102, 0013, 0006, 0001, 0406"

    }

    command "levelOpenClose"
    
    preferences {
    		input name: "mode", type: "bool", title: "Xiaomi Curtain Direction Set", description: "Reverse Mode ON", required: true,
          	displayDuringSetup: true
				}    

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }

		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}



        multiAttributeTile(name: "windowShade", type: "generic", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState("closed", label: 'closed', action: "windowShade.open", icon: "st.doors.garage.garage-closed", backgroundColor: "#A8A8C6", nextState: "opening")
                attributeState("open", label: 'open', action: "windowShade.close", icon: "st.doors.garage.garage-open", backgroundColor: "#F7D73E", nextState: "closing")
                attributeState("closing", label: '${name}', action: "windowShade.open", icon: "st.contact.contact.closed", backgroundColor: "#B9C6A8")
                attributeState("opening", label: '${name}', action: "windowShade.close", icon: "st.contact.contact.open", backgroundColor: "#D4CF14")
                attributeState("partially open", label: 'partially\nopen', action: "windowShade.close", icon: "st.doors.garage.garage-closing", backgroundColor: "#D4ACEE", nextState: "closing")
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action: "switch level.setLevel")
            }
        }
               
        
//        standardTile("switch", "device.switch") {
//            state("on", label: 'open', action: "switch.off", icon: "st.doors.garage.garage-open", backgroundColor: "#ffcc33")
//            state("off", label: 'closed', action: "switch.on", icon: "st.doors.garage.garage-closed", backgroundColor: "#bbbbdd")
//        }
        standardTile("open", "open", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("open", label: 'open', action: "windowShade.open", icon: "st.contact.contact.open")
        }
        standardTile("close", "close", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("close", label: 'close', action: "windowShade.close", icon: "st.contact.contact.closed")
        }
        standardTile("refresh", "command.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: " ", action: "refresh.refresh", icon: "https://www.shareicon.net/data/128x128/2016/06/27/623885_home_256x256.png"
        }
        main(["windowShade"])
        details(["windowShade", "open", "refresh", "close", "level"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
    def event = zigbee.getEvent(description)

    try {
        if (parseMap.raw.startsWith("0104")) {
            log.debug "Xiaomi Curtain"
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"

        } else if (parseMap.raw.endsWith("0007")) {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
            log.debug "running…"
        } else if (parseMap.endpoint.endsWith("01")) {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
            if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {

                long theValue = Long.parseLong(parseMap["value"], 16)
                def eventStack = []
             if(mode == true) {
                if (theValue > 0x42c70000) {
                    log.debug "Just Closed"
                    eventStack.push(createEvent(name: "windowShade", value: "closed"))
                    eventStack.push(createEvent(name: "switch", value: "off"))
                    eventStack.push(createEvent(name: "close", value: "close"))
                    eventStack.push(createEvent(name: "level", value: "0"))
                } else if (theValue > 0) {
                    String hex = parseMap["value"]
                    Long i = Long.parseLong(hex, 16);
                    Float f = Float.intBitsToFloat(i.intValue());
                    log.debug f + '% Partially Open'
                    eventStack.push(createEvent(name: "windowShade", value: "partially open"))
                    eventStack.push(createEvent(name: "switch", value: "on"))
                    eventStack.push(createEvent(name: "level", value: 100 - f))
                } else {
                   log.debug "Just Fully Open"
                    eventStack.push(createEvent(name: "windowShade", value: "open"))
                    eventStack.push(createEvent(name: "switch", value: "on"))
                    eventStack.push(createEvent(name: "level", value: "100"))
                }
             }
             else {
               if (theValue > 0x42c70000) {
                    log.debug "Just Fully Open"
                    eventStack.push(createEvent(name: "windowShade", value: "open"))
                    eventStack.push(createEvent(name: "switch", value: "on"))
                    eventStack.push(createEvent(name: "level", value: "100"))
                    
                } else if (theValue > 0) {
                    String hex = parseMap["value"]
                    Long i = Long.parseLong(hex, 16);
                    Float f = Float.intBitsToFloat(i.intValue());
                    log.debug f + '% Partially Open'
                    eventStack.push(createEvent(name: "windowShade", value: "partially open"))
                    eventStack.push(createEvent(name: "switch", value: "on"))
                    eventStack.push(createEvent(name: "level", value: f))
                } else {
                    log.debug "Just Closed"
                    eventStack.push(createEvent(name: "windowShade", value: "closed"))
                    eventStack.push(createEvent(name: "switch", value: "off"))
                    eventStack.push(createEvent(name: "close", value: "close"))
                    eventStack.push(createEvent(name: "level", value: "0"))
                }
			}
                return eventStack
            }
        } else {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        }

        if (event["name"] == "switch") {
            return createEvent(name: "switch", value: event["value"])
        }
    } catch (Exception e) {
        log.warn e
    }
                log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"

}

def close() {
    log.debug "Set Close"
	if(mode == true){
    zigbee.command(0x0006, 0x01)
    } else {
    zigbee.command(0x0006, 0x00)
    }
}

def open() {
    log.debug "Set Open"
	if(mode == true){
    zigbee.command(0x0006, 0x00)
    } else {
    zigbee.command(0x0006, 0x01)
    }
}

def off() {
    log.debug "off()"
	if(mode == true){
    zigbee.command(0x0006, 0x01)
    } else {
    zigbee.command(0x0006, 0x00)
    }
}

def on() {
    log.debug "on()"
	if(mode == true){
    zigbee.command(0x0006, 0x00)
    } else {
    zigbee.command(0x0006, 0x01)
    }
}

def setLevel(level) {
	if(mode == true){
    	if(level == 100) {
        log.debug "Set Open"
        zigbee.command(0x0006, 0x00)
        }
		else if(level < 1) {
        	log.debug "Set Close"
       	 	zigbee.command(0x0006, 0x01)
        	}
     	else {
        	log.debug "Set Level: ${level}%"
         	def f = 100 - level
        	String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
        	zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
    }  }
	else{
    if (level > 0) {
        log.debug "Set Level: ${level}%"
        String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
        zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
    } else {
        log.debug "Set Close"
        zigbee.command(0x0006, 0x00)
    } }
}

def refresh() {
    log.debug "refresh()"
//    "st rattr 0x${device.deviceNetworkId} ${1} 0x000d 0x0055"
     zigbee.readAttribute(0x000d, 0x0055)
}