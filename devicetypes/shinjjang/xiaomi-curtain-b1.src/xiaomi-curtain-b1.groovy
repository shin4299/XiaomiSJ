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

import physicalgraph.zigbee.zcl.DataType
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus

metadata {
    definition(name: "Xiaomi Curtain B1", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.blind", vid: "generic-shade") {
      capability "Window Shade" 
//      capability "Window Shade Preset"
      capability "Switch Level"
      capability "Switch"
      capability "Actuator"
	  capability "Battery"
      capability "Power Source"
      capability "Health Check"
      capability "Configuration"
      capability "Refresh"
        
    command "levelOpenClose"
    command "Pause"       

        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
    }


    
    preferences {
          input name: "mode", type: "bool", title: "Xiaomi Curtain Direction Set", description: "Reverse Mode ON", required: true,
             displayDuringSetup: true
   }    

    tiles(scale: 2) {
        multiAttributeTile(name: "windowShade", type: "generic", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState("closed", label: 'closed', action: "windowShade.open", icon: "st.doors.garage.garage-closed", backgroundColor: "#A8A8C6", nextState: "opening")
                attributeState("open", label: 'open', action: "windowShade.close", icon: "st.doors.garage.garage-open", backgroundColor: "#F7D73E", nextState: "closing")
                attributeState("closing", label: '${name}', action: "windowShade.open", icon: "st.contact.contact.closed", backgroundColor: "#B9C6A8")
                attributeState("opening", label: '${name}', action: "windowShade.close", icon: "st.contact.contact.open", backgroundColor: "#D4CF14")
                attributeState("partially open", label: 'partially\nopen', action: "windowShade.close", icon: "st.doors.garage.garage-closing", backgroundColor: "#D4ACEE", nextState: "closing")
            }
            tileAttribute ("powerSource", key: "SECONDARY_CONTROL") {
                attributeState "powerSource", label:'Power Source: ${currentValue}'
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action: "setLevel")
            }
        }
        standardTile("open", "open", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("open", label: 'open', action: "windowShade.open", icon: "st.contact.contact.open")
        }
        standardTile("close", "close", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("close", label: 'close', action: "windowShade.close", icon: "st.contact.contact.closed")
        }
        standardTile("stop", "stop", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("stop", label: 'stop', action: "Pause", icon: "st.illuminance.illuminance.dark")
        }
        standardTile("refresh", "command.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: " ", action: "refresh.refresh", icon: "https://www.shareicon.net/data/128x128/2016/06/27/623885_home_256x256.png"
        }
        valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }

        main(["windowShade"])
        details(["windowShade", "open", "stop", "close", "battery", "refresh"])
    }
}

private getCLUSTER_BASIC() { 0x0000 }
private getCLUSTER_POWER() { 0x0001 }
private getCLUSTER_WINDOW_COVERING() { 0x0102 }
private getCLUSTER_WINDOW_POSITION() { 0x000d }
private getBASIC_ATTR_POWER_SOURCE() { 0x0007 }
private getPOWER_ATTR_BATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getPOSITION_ATTR_VALUE() { 0x0055 }
private getCOMMAND_OPEN() { 0x00 }
private getCOMMAND_CLOSE() { 0x01 }
private getCOMMAND_PAUSE() { 0x02 }
private getENCODING_SIZE() { 0x39 }

// Parse incoming device messages to generate events
def parse(String description) {
	def parseMap = zigbee.parseDescriptionAsMap(description)
	Map map = zigbee.getEvent(description)
    
	if(map.name == "powerSource") {
		log.info "powersource: ${map.value}"
		def result = map ? createEvent(map) : [:]
		return result
	}
	
	def curtainLevel = null

	if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {
		if (parseMap["size"] == "16") {
			long theValue = Long.parseLong(parseMap["value"], 16)
			float floatValue = Float.intBitsToFloat(theValue.intValue());
			log.debug "long => ${theValue}, float => ${floatValue}"
			curtainLevel = floatValue.intValue()
			levelEvent(curtainLevel)
		} else if (parseMap["size"] == "28" && parseMap["value"] == "00000000") {
			log.debug "done…"
			sendHubCommand(zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE))                
		}
	} else if (parseMap["cluster"] == "0001" && parseMap["attrId"] == "0021") {
		def bat = parseMap["value"]
		long value = Long.parseLong(bat, 16)/2
		log.info "Battery: ${value}%, ${bat}"
		def result = createEvent(name:"battery", value: value)
		return result
	} else if (parseMap["clusterId"] == "000D") {
		log.debug "Xiaomi Curtain B1"
	} else {
		log.warn "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
	}
}

def levelEvent(curtainLevel) {
	def windowShadeStatus = ""
	if(mode == true) {
		if (curtainLevel == 100) {
			log.info "Just Closed"
			windowShadeStatus = "closed"
			curtainLevel = 0
		} else if (curtainLevel == 0) {
			log.info "Just Fully Open"
			windowShadeStatus = "open"
			curtainLevel = 100
		} else {
			windowShadeStatus = "partially open"
			curtainLevel = 100 - curtainLevel
            log.info curtainLevel + '% Partially Open'
		}
	} else {
		if (curtainLevel == 100) {
			log.info "Just Fully Open"
			windowShadeStatus = "open"
			curtainLevel = 100
		} else if (curtainLevel > 0) {
			log.info curtainLevel + '% Partially Open'
			windowShadeStatus = "partially open"
			curtainLevel = curtainLevel
		} else {
			log.info "Just Closed"
			windowShadeStatus = "closed"
			curtainLevel = 0
		}
	}
	def eventStack = []
	eventStack.push(createEvent(name:"windowShade", value: windowShadeStatus as String))
	eventStack.push(createEvent(name:"level", value: curtainLevel))
	eventStack.push(createEvent(name:"switch", value: (windowShadeStatus == "closed" ? "off" : "on")))
	return eventStack                
}

def updated() {
}	

def close() {
    log.info "close()"
	setLevel(0)    
}

def open() {
    log.info "open()"
	setLevel(100)    
}

def on() {
	setLevel(100)
}


def off() {
	setLevel(0)
}


def Pause() {
    log.info "stop()"
	zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
}

def setLevel(level) {
    if (level == null) {level = 0}
    level = level as int
    Integer  currentLevel = device.currentValue("level")
    if (level > currentLevel) {
        sendEvent(name: "windowShade", value: "opening")
    } else if (level < currentLevel) {
        sendEvent(name: "windowShade", value: "closing")
    }
	if(mode == true){
		log.info "Set Level: ${level}%"
		def f = (100 - level) as int
		String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
		zigbee.writeAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE, ENCODING_SIZE, hex)
	} else{
		log.info "Set Level: ${level}%"
		String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
		zigbee.writeAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE, ENCODING_SIZE, hex)
	}
}

def refresh() {
    log.info "refresh()"
    def cmds = []
    cmds += zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
    cmds += zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING)
    cmds += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    return cmds
}

def ping() {
    return refresh()
}

def configure() {
    log.info "configure()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    log.debug "Configuring Reporting and Bindings."

    return refresh()
}
