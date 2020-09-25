/**
 *  Virtual Keypad Number Button Child
 *
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
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 * 
 */
metadata {
	definition (name: "Virtual Keypad Number Button Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualKeypadNumberButtonChild.groovy") {
		capability "PushableButton"
		capability "Momentary"
		capability "Actuator"
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
	
	command "push"
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)
}

def push(evt) {
	if (logEnable) log.debug "push(${evt}) called"
	sendEvent(name: "pushed", value: evt, isStateChange  : true)
	parent.buttonPress("${evt}")
	
	//sendEvent(name: "pushed", value: "1", isStateChange  : true)
	//def btn = device.deviceNetworkId.split("-")[-1]
	//parent.buttonPress("${btn}")
}


/*
def parse(String description) {
	log.debug "parse(${description}) called"
    if (logEnable) log.debug "parse(${description}) called"
	//def parts = description.split(" ")
    //def name  = parts.length>0?parts[0].trim():null
    //def value = parts.length>1?parts[1].trim():null
    //if (name && value) {
        // Update device
        sendEvent(name: "pushed", value: "1", isStateChange  : true)
    //}
    //else {
    //	log.error "Missing either name or value.  Cannot parse!"
    //}
}
*/



/*
def on() {
    sendData("on")
}

def off() {
    sendData("off")
}

def sendData(String value) {
    def name = device.deviceNetworkId.split("-")[-1]
    parent.sendData("${name} ${value}")  
}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
    if (name && value) {
        // Update device
        sendEvent(name: name, value: value)
    }
    else {
    	log.error "Missing either name or value.  Cannot parse!"
    }
}
*/

