/**
 *  Virtual Keypad Button Child
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
 * 	 09-26-20	mbarone			initial release
 * 	 10-01-20	mbarone			added panic option with tamper trigger
 * 	 10-16-20	mbarone			bugfix in buttonpress logic
 * 	 02-10-21	mbarone			changed auto turn off to 500 milliseconds instead of 5 seconds
 * 	 02-15-21	mbarone			added ability to press button when this is a chime device and it will trigger the chime on() function to trigger an outside source
 */
 
def setVersion(){
	state.version = "0.0.5"
} 
 
 
metadata {
	definition (name: "Virtual Keypad Button Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualKeypadButtonChild.groovy") {
		capability "PushableButton"
		capability "Momentary"
		capability "Actuator"
		capability "Switch"
		capability "TamperAlert"
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
	off()
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)
}

def push(evt) {
	def btn = device.deviceNetworkId.split("-")[-1]
	if (logEnable) log.debug "push(${evt}) called using ${btn}"
	if(btn == "Clear"){
		sendEvent(name: "pushed", value: "1", isStateChange  : true)
		parent.buttonPress("${btn}")
	} else if(btn == "Panic"){
		sendEvent(name: "pushed", value: "1", isStateChange  : true)
		parent.buttonPress("${btn}")
	} else if(btn == "Chime"){
		on()		
	} else if(btn == "Number" || btn == "Button"){
		if(evt==null){
			return
		}
		sendEvent(name: "pushed", value: evt, isStateChange  : true)
		parent.buttonPress("${evt}")		
	} else {
		sendEvent(name: "pushed", value: "1", isStateChange  : true)
		btn = device.deviceNetworkId.split("-")[-2]+"-"+btn
		parent.buttonPress("${btn}")
	}
}

def on() {
	if (logEnable) log.debug "on() called"
	sendEvent(name: "switch", value: "on")
	runInMillis(500,off)
}

def off() {
	if (logEnable) log.debug "off() called"
	sendEvent(name: "switch", value: "off")
}

def tamperAlert(){
	if (logEnable) log.debug "tamperAlert() called"
	sendEvent(name: "tamper", value: "detected", isStateChange  : true)
	runInMillis(500,clearTamperAlert)
}

def clearTamperAlert(){
	if (logEnable) log.debug "clearTamperAlert() called"
	sendEvent(name: "tamper", value: "clear", isStateChange  : true)
}
