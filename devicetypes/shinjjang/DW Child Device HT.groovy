/**
 *  DW Child Device TH
 *
 *  Copyright 2019 ShinJjang
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
	definition (name: "DW Child Device TH", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.switch", mnmn: "SmartThings", vid: "generic-switch") {
	capability "Temperature Measurement"
	capability "Relative Humidity Measurement"
	capability "Sensor"
	capability "Battery"
	capability "Health Check"
	}

    tiles(scale: 2) {
        multiAttributeTile(name:"temperature", type:"generic", width:6, height:4) {
            tileAttribute("device.temperature", key:"PRIMARY_CONTROL"){
                attributeState("temperature", label:'${currentValue}°',
					backgroundColors:[
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
            tileAttribute("device.multiAttributesReport", key: "SECONDARY_CONTROL") {
                attributeState("multiAttributesReport", label:'${currentValue}' //icon:"st.Weather.weather12",
                )
            }
        }
        valueTile("temperature2", "device.temperature", inactiveLabel: false) {
        	state "temperature", label:'${currentValue}°', icon: "st.Weather.weather2",
        		backgroundColors:[
 	    			[value: 31, color: "#153591"],
 	    			[value: 44, color: "#1e9cbb"],
 	    			[value: 59, color: "#90d2a7"],
 	    			[value: 74, color: "#44b621"],
 	    			[value: 84, color: "#f1d801"],
 	    			[value: 95, color: "#d04e00"],
 	    			[value: 96, color: "#bc2323"]
 	    		]
            }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiHumidity.png",
            backgroundColors:[
                [value: 0, color: "#FFFCDF"],
                [value: 4, color: "#FDF789"],
                [value: 20, color: "#A5CF63"],
                [value: 23, color: "#6FBD7F"],
                [value: 56, color: "#4CA98C"],
                [value: 59, color: "#0072BB"],
                [value: 76, color: "#085396"]
            ]
        }
    }
}


def installed() {
	// This is set to a default value, but it is the responsibility of the parent to set it to a more appropriate number
	sendEvent(name: "checkInterval", value: 30 * 60, displayed: false, data: [protocol: "zigbee"])
}

void on() {
   log.debug("on")
	parent.childOn(device.deviceNetworkId)
}

void off() {
   log.debug("off")
	parent.childOff(device.deviceNetworkId)
}

def ping() {
   log.debug("ping")
	parent.childRefresh(device.deviceNetworkId)
}

def refresh() {
   log.debug("refresh")
	parent.childRefresh(device.deviceNetworkId)
}

def uninstalled() {
	parent.delete()
}
