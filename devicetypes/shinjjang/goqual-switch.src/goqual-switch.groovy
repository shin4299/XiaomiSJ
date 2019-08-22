/*
 *  Copyright 2018 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *  Date : 2018-11-28
 */

metadata {
    definition(name: "GoQual Switch", namespace: "shinjjang", author: "shinjjang", ocfDeviceType: "oic.d.switch", mnmn: "SmartThings", vid: "generic-switch") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Health Check"
        capability "Switch"

        command "childOn", ["string"]
        command "childOff", ["string"]
		command "childRefresh"

        fingerprint profileId: "0104", deviceId: "0100", endpoint: "01", inClusters: "0006, 0000, 0003", outClusters: "0019", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
        fingerprint profileId: "0104", deviceId: "0100", endpoint: "02", inClusters: "0006, 0000, 0003", outClusters: "0019", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
        fingerprint profileId: "0104", deviceId: "0100", endpoint: "03", inClusters: "0006, 0000, 0003", outClusters: "0019", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
        
        fingerprint profileId: "0104", deviceId: "0100", endpoint: "01", inClusters: "0006, 0000, 0003", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
        fingerprint profileId: "0104", deviceId: "0100", endpoint: "02", inClusters: "0006, 0000, 0003", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
        fingerprint profileId: "0104", deviceId: "0100", endpoint: "03", inClusters: "0006, 0000, 0003", manufacturer: "", model: "", deviceJoinName: "GQ Switch"
    }
    // simulator metadata
    simulator {
        // status messages
        status "on": "on/off: 1"
        status "off": "on/off: 0"

        // reply messages
        reply "zcl on-off on": "on/off: 1"
        reply "zcl on-off off": "on/off: 0"
    }
	preferences {
		input name: "stateins", type: "bool", title: "스테이트 ins 값 변경", description:"NOTE: 설치후 차일드 디바이스가 Unavailable 될때, 또는 장치화면에 'NOK'가 뜨면 켜고 저장"
        }
    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        standardTile("state", "state", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "0", label: "NOK"
            state "1", label: "OK"
        }
        main "switch"
        details(["switch", "refresh", "state"])
    }
}

def installed() {
    state.ins = 0
    state.ch = 0
    state.sep = 0
    log.debug "install Channel=${state.ch} ins=${state.ins} sep=${state.sep}"
    updateDataValue("onOff", "catchall")
//    sendHubCommand(refresh().collect { new physicalgraph.device.HubAction(it) }, 0)
//	healthPoll()
}

def updated() {
	if(stateins) {
		state.ins = 1
		device.updateSetting("stateins", false)
	}
	sendEvent(name: "state", value: state.ins, displayed: false)
}

def parse(String description) {
    log.debug "description is $description"
    Map map = zigbee.getEvent(description)
    log.debug "map is $map"
    if (map) {
        if (description ?.startsWith('on/off')) {
            log.debug "receive on/off message without endpoint id"
            sendHubCommand(refresh().collect { new physicalgraph.device.HubAction(it) }, 0)
        } else {
            Map descMap = zigbee.parseDescriptionAsMap(description)
            log.debug "$descMap"
            def ep = descMap.sourceEndpoint as int
            log.debug "descMapEP=${ep}"
            log.debug "parse Channel=${state.ch} ins=${state.ins} sep=${state.sep}"

            if (state.ins == 0 || state.ins == null ) {
                if (state.ch < ep) {
                    state.ch = ep
                    log.debug "search Channel=${state.ch}"
                    sendHubCommand(refresh().collect { new physicalgraph.device.HubAction(it) }, 0)
                } else if (state.ch == ep) {
                    if (state.sep == 3) {
                        state.ins = 1
				        createChildDevices()
                    } else {
                        state.sep = state.sep + 1
                        log.debug "Search temp time=${state.sep}"
                        sendHubCommand(refresh().collect { new physicalgraph.device.HubAction(it) }, 0)
                    }
                } else {
                }

            } else {

                if (descMap ?.clusterId == "0006" && descMap.sourceEndpoint == "01") {
                    sendEvent(map)
                } else if (descMap ?.clusterId == "0006") {
                    def childDevice = childDevices.find {
                        it.deviceNetworkId == "$device.deviceNetworkId:${descMap.sourceEndpoint}"
                    }
                    if (childDevice) {
                        childDevice.sendEvent(map)
                    log.debug "&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&childDevice=${childDevice}"
                    }
                }
            }
        }
    }
    sendEvent(name: "state", value: state.ins, displayed: false)
}



private void createChildDevices() {
	if (state.ch != 1) {
    	for (i in 2..state.ch) {
        	addChildDevice("GoQual Child Switch", "${device.deviceNetworkId}:0${i}", device.hubId,
            	[completedSetup: true, label: "${device.displayName.split("1")[-1]}${i}", isComponent : false])
    	}
    }
}

private getChildEndpoint(String dni) {
    dni.split(":")[-1] as Integer
}

def on() {
    log.debug("on")
    zigbee.on()
}

def off() {
    log.debug("off")
    zigbee.off()
}

def childOn(String dni) {
    log.debug(" child on ${dni}")
    zigbee.command(0x0006, 0x01, "", [destEndpoint: getChildEndpoint(dni)])
//	stateRefresh(dni)
}

def childOff(String dni) {
    log.debug(" child off ${dni}")
    zigbee.command(0x0006, 0x00, "", [destEndpoint: getChildEndpoint(dni)])
//	stateRefresh(dni)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return refresh()
}

def checkState(dni) {
    log.debug(" child checkState ${dni}")
    	sendHubCommand(childRefresh(dni))
}

def stateRefresh(String dni) {
    log.debug(" child stateRefresh ${dni}")
	runIn(10, checkState(dni))	
}

def refresh() {
    return zigbee.readAttribute(0x0006, 0x0000, [destEndpoint: 0xFF])
}

def childRefresh(String dni) {
    log.debug(" child refresh ${dni}")
    return zigbee.readAttribute(0x0006, 0x0000, [destEndpoint: getChildEndpoint(dni)])
}

def poll() {
    refresh()
}

def healthPoll() {
    log.debug "healthPoll()"
    def cmds = refresh()
    cmds.each { sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def configureHealthCheck() {
    Integer hcIntervalMinutes = 12
    if (!state.hasConfiguredHealthCheck) {
        log.debug "Configuring Health Check, Reporting"
        unschedule("healthPoll")
        runEvery5Minutes("healthPoll")
        def healthEvent = [name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
        // Device-Watch allows 2 check-in misses from device
        sendEvent(healthEvent)
        childDevices.each {
            it.sendEvent(healthEvent)
        }
        state.hasConfiguredHealthCheck = true
    }
}

def configure() {
    log.debug "configure()"
    configureHealthCheck()
    //the switch will send out device anounce message at ervery 2 mins as heart beat,setting 0x0099 to 1 will disable it.
    return zigbee.writeAttribute(0x0000, 0x0099, 0x20, 0x01, [mfgCode: 0x0000])
}
