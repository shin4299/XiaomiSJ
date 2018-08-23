/**
 *  Xiaomi Smart Socket
 *
 *  Copyright 2017 Shin
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
 */
metadata {
	definition (name: "Xiaomi Plug SJ", namespace: "ShinJjang", author: "ShinJjang", mnmn: "SmartThings", vid:"generic-switch-power-energy") {
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Energy Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Outlet"
        
        attribute "Power", "string"
        attribute "Volt", "string"
        attribute "energy", "string"
        attribute "temp", "string"

		command "power"
		command "temp"
		command "energy"
		command "volt"
		command "Outlet"


		fingerprint endpointId: "0x01", profileId: "0x0104", deviceId: "0x0051", inClusters: "0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0010, 0x000A", outClusters: "0x0000, 0x0004", manufacturer: "LUMI", model: "lumi.ctrl_86plug", deviceJoinName: "Xiaomi Smart Socket"
		fingerprint endpointId: "0x02", profileId: "0x0104", deviceId: "0x0009", inClusters: "0x000C", outClusters: "0x000C, 0xBA00", manufacturer: "LUMI", model: "lumi.ctrl_86plug", deviceJoinName: "Xiaomi Smart Socket"
		fingerprint endpointId: "0x03", profileId: "0x0104", deviceId: "0x0053", inClusters: "0x000C", outClusters: "0x000C", manufacturer: "LUMI", model: "lumi.ctrl_86plug", deviceJoinName: "Xiaomi Smart Socket"
		fingerprint endpointId: "0x64", profileId: "0x0104", deviceId: "0x0107", inClusters: "0x000F", outClusters: "0x000F, 0xB700", manufacturer: "LUMI", model: "lumi.ctrl_86plug", deviceJoinName: "Xiaomi Smart Socket"

	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: 'Turning On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: 'Turning Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label: '${currentValue}W'
			}
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: '', action: "refresh.refresh", icon: "https://www.shareicon.net/data/128x128/2016/06/29/624555_refresh_256x256.png"
		}        
        valueTile("power", "device.power", width:4, height:2, inactiveLabel: false, decoration: "flat" ) {
        	state "power", label: 'Power ${currentValue}W', action: "power", defaultState: true
		}
        valueTile("Volt", "device.volt", width:2, height:2, inactiveLabel: false, decoration: "flat" ) {
        	state "volt", label: 'Volt\n${currentValue}V', action: "volt", defaultState: true
		}        
        valueTile("energy", "device.energy", width:2, height:2, inactiveLabel: false, decoration: "flat" ) {
        	state "energy", label: 'Energy\n${currentValue}KWh', action: "energy", defaultState: true
		}
        valueTile("Temp", "device.temp", width:2, height:2, inactiveLabel: false, decoration: "flat" ) {
        	state "temp", label: 'Temp\n${currentValue}°C', action: "temp", defaultState: true
		}

		main "switch"
        details(["switch", "power", "Temp", "Volt", "energy", "refresh"])
	}
}

def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
    def event = zigbee.getEvent(description)

try {
        if(parseMap["cluster"] == "000C" && parseMap["attrId"] == "0055" && parseMap["endpoint"] == "02") {
                   String hex = parseMap["value"]
			        Long i = Long.parseLong(hex, 16);
			        Float f = Float.intBitsToFloat(i.intValue());
                    log.debug f
                    sendEvent(name: "power", value: f)
                    }
                    
        else if(parseMap["cluster"] == "000C" && parseMap["attrId"] == "0055" && parseMap["endpoint"] == "03") {
                    String hex = parseMap["value"]
			        Long i = Long.parseLong(hex, 16);
			        Float f = Float.intBitsToFloat(i.intValue());
                    log.debug f
                    sendEvent(name: "energy", value: f)
                    }
                    
        else if(parseMap["cluster"] == "0001" && parseMap["attrId"] == "0000" && parseMap["endpoint"] == "01") {
                    String hex = parseMap["value"]
			        Long i = Long.parseLong(hex, 16);
                    log.debug i/10
                    sendEvent(name: "volt", value: i/10 )
                    }
                    
        else if(parseMap["cluster"] == "0002" && parseMap["attrId"] == "0000" && parseMap["endpoint"] == "01") {
                   String hex = parseMap["value"]
                   Long i = Long.parseLong(hex, 16)
                   def a = i - 32
                   def temp = a + 20
                    log.debug temp
                   sendEvent(name: "temp", value: temp )
                    }    
        else if(parseMap["cluster"] == "000A" && parseMap["endpoint"] == "01") {
			    			log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
}                                       
        else if(parseMap["clusterId"] == "0000" && parseMap["attrId"] == "FF01") {
         			def valueData = parseMap["value"]
                    def eventStack = []
                    def vol = valueData[54,55,56,57];
                    def wk = valueData[62,63,64,65,66,67,68,69];
                    def pw = valueData[74,75,76,77,78,79,80,81];
                    def tem = valueData[86,87];
                    def onOffValue = valueData[93];
                    log.debug onOffValue + vol + wk + pw + tem
                                       
                    String hexv = vol
			        Long iv = Long.parseLong(hexv, 16);
                    
                    String hexw = wk
			        Long iw = Long.parseLong(hexw, 16);
			        Float fw = Float.intBitsToFloat(iw.intValue());
                    
                    String hexp = pw
			        Long ip = Long.parseLong(hexp, 16);
			        Float fp = Float.intBitsToFloat(ip.intValue());

                    String hext = tem
                    Long it = Long.parseLong(hext, 16)
                    def at = it - 32
                    def tempr = at + 20
                                        
                    log.debug onOffValue + "/" + iv + "/" + fw + "/" + fp + "/" + tempr
                    
                    sendEvent(name: "volt", value: iv/10 )
                    sendEvent(name: "temp", value: tempr )
                    sendEvent(name: "energy", value: fw)
                    sendEvent(name: "power", value: fp)
                    sendEvent(name: "switch", value: onOffValue == "1" ? "on":"off")
                    }

		else if (event) {
    		log.info "Event Acc: ${event}"
			if (event.name == "switch") {
			    			log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"

            def descriptionText = event.value == "on" ? '{{ device.displayName }} is On' : '{{ device.displayName }} is Off'
			event = createEvent(name: event.name, value: event.value, descriptionText: descriptionText, translatable: true)
            return event
			}
//        log.warn "Unhandled Event - description: ${description}, event: ${event}"
        
			}
        else {
    			log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
                }
    } catch(Exception e) {
        log.warn e
    }
        			log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"

}



def off() {
    log.debug "off()"
	"st cmd 0x${device.deviceNetworkId} 1 6 0 {}"
}

def on() {
   log.debug "on()"
	"st cmd 0x${device.deviceNetworkId} 1 6 1 {}"
}

def power() {
	"st rattr 0x${device.deviceNetworkId} ${2} 0x000C 0x0055"
}
def energy() {
    "st rattr 0x${device.deviceNetworkId} ${3} 0x000C 0x0055"
}
def temp() {
//       zigbee.writeAttribute(0x0005, 0xf0000, 0x20, 0x01)
//    "st rattr 0x${device.deviceNetworkId} ${1} 0x000A 0x0055"
    "st rattr 0x${device.deviceNetworkId} ${1} 0x0005 0x0001"
   log.debug "temp()"

}
def volt() {
   log.debug "volt()"
	"st cmd 0x${device.deviceNetworkId} 2 6 1 {}"
//	"st cmd 0x${device.deviceNetworkId} 1 6 8 {}"
//    "st rattr 0x${device.deviceNetworkId} ${1} 0x000A 0x0001"
}

def refresh() {
    def endpointId = 2
    def delay = 50
    def cmds = []
    
    cmds << "st rattr 0x${device.deviceNetworkId} ${2} 0x000C 0x0055"
    cmds << "delay ${delay}"
    
    cmds << "st rattr 0x${device.deviceNetworkId} ${3} 0x000C 0x0055"
    cmds << "delay ${delay}"
        
    cmds << "st rattr 0x${device.deviceNetworkId} ${1} 0x0001 0x0000"
    cmds << "delay ${delay}"
    
    cmds << "st rattr 0x${device.deviceNetworkId} ${1} 0x0002 0x0000"
    cmds << "delay ${delay}"

    cmds << "st rattr 0x${device.deviceNetworkId} ${1} 0x0006 0x0000"
    cmds << "delay ${delay}"

    log.info "refresh operaion requested"
    
    return cmds
}
