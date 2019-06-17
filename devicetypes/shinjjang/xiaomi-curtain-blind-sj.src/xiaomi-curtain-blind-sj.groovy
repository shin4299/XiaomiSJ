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
    definition(name: "Xiaomi Curtain & Blind SJ ", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.blind") {
      capability "Window Shade" 
      capability "Window Shade Preset"
      capability "Switch"
      capability "Switch Level"
      capability "Actuator"
      capability "Health Check"
      capability "Sensor"
      capability "Refresh"
      
    command "levelOpenClose"
    command "shadeAction"
    command "Pause"       

        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013, 0006, 0001, 0406", outClusters: "0019, 000A, 000D, 0102, 0013, 0006, 0001, 0406", manufacturer: "LUMI", model: "lumi.curtain", deviceJoinName: "Xiaomi Curtain V1"
//        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain V2"
        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013", outClusters: "000A", manufacturer: "LUMI", model: "lumi.curtain.aq2", deviceJoinName: "Xiaomi Blind"

    }


    
    preferences {
          input name: "mode", type: "bool", title: "Xiaomi Curtain Direction Set", description: "Reverse Mode ON", required: true,
             displayDuringSetup: true
          input name: "openInt", type: "integer", title: "Open Percetage(**% 열림)", defaultValue: 30, description: "**% 열림을 설정합니다.", required: true
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
        standardTile("home", "device.level", width: 2, height: 2, decoration: "flat") {
            state "default", label: "home", action:"presetPosition", icon:"st.Home.home2"
        }

        main(["windowShade"])
        details(["windowShade", "open", "stop", "close", "refresh"])
    }
}
// Parse incoming device messages to generate events
def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
    def event = zigbee.getEvent(description)

    try {
        if (parseMap.raw.startsWith("0104")) {
        } else if (parseMap.raw.endsWith("0007")) {
            log.debug "running…"
        } else if (parseMap.endpoint.endsWith("01")) {
            if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {
                long theValue = Long.parseLong(parseMap["value"], 16)
                float floatValue = Float.intBitsToFloat(theValue.intValue());
                def windowShadeStatus = ""
            int curtainLevel = floatValue.intValue()
                if(mode == true) {
                    if (theValue > 0x42c70000) {
                        log.debug "Just Closed"
                        windowShadeStatus = "closed"
                        curtainLevel = 0
                    } else if (theValue > 0) {
                        log.debug curtainLevel + '% Partially Open'
                        windowShadeStatus = "partially open"
                        curtainLevel = 100 - floatValue.intValue()
                    } else {
                        log.debug "Just Fully Open"
                        windowShadeStatus = "open"
                        curtainLevel = 100
                    }
                } else {
                    if (theValue > 0x42c70000) {
                        log.debug "Just Fully Open"
                        windowShadeStatus = "open"
                        curtainLevel = 100
                    } else if (theValue > 0) {
                        log.debug curtainLevel + '% Partially Open'
                        windowShadeStatus = "partially open"
                        curtainLevel = floatValue.intValue()
                    } else {
                        log.debug "Just Closed"
                        windowShadeStatus = "closed"
                        curtainLevel = 0
                    }
                }

                def eventStack = []
                eventStack.push(createEvent(name:"windowShade", value: windowShadeStatus as String))
                eventStack.push(createEvent(name:"level", value: curtainLevel))

                return eventStack
            }
        } else {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        }

    } catch (Exception e) {
        log.warn e
    }
}

def updated() {
	sendEvent(name: "openlevel", value: openInt)
}	

def close() {
    log.debug "close()"
    setLevel(0)
//   zigbee.command(0x0102, 0x01)
}

def open() {
    log.debug "open()"
    setLevel(100)
   //zigbee.command(0x0102, 0x00)
}

def off() {
    log.debug "close()"
    setLevel(0)
//   zigbee.command(0x0102, 0x01)
}

def on() {
    log.debug "open()"
    setLevel(100)
//   zigbee.command(0x0102, 0x00)
}

def Pause() {
    log.debug "stop()"
   zigbee.command(0x0102, 0x02)
}

def setLevel(level) {
    if (level == null) {level = 0}
    level = level as int
    
   if(mode == true){
       if(level == 100) {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x00)
        } else if(level < 1) {
           log.debug "Set Open"
              zigbee.command(0x0102, 0x01)
        } else {
           log.debug "Set Level: ${level}%"
            def f = 100 - level
           String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
           zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
       }  
    } else{
       if (level == 100){
            log.debug "Set Open"
            zigbee.command(0x0102, 0x00)
        } else if(level > 0) {
            log.debug "Set Level: ${level}%"
            String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
            zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
        } else {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x01)
        } 
    }
}

def shadeAction(level) {
   if(mode == true){
       if(level == 100) {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x00)
        } else if(level < 1) {
           log.debug "Set Open"
              zigbee.command(0x0102, 0x01)
        } else {
           log.debug "Set Level: ${level}%"
            def f = 100 - level
           String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
           zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
       }  
    } else{
       if (level == 100){
            log.debug "Set Open"
            zigbee.command(0x0102, 0x00)
        } else if(level > 0) {
            log.debug "Set Level: ${level}%"
            String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
            zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
        } else {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x01)
        } 
    }
}
def refresh() {
    log.debug "refresh()"
     zigbee.readAttribute(0x000d, 0x0055)
     }