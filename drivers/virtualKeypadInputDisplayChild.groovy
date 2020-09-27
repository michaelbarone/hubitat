/**
 *  Virtual Keypad Input Display Child
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
 * 	 9-26-20	mbarone			initial release
 */
metadata {
	definition (name: "Virtual Keypad Input Display Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualKeypadInputDisplayChild.groovy") {
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
	
	attribute "InputDisplay","string"
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    sendEvent(name: "InputDisplay", value: "Enter Code", displayed: false)
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)
}

def updateInputDisplay(text){
	sendEvent(name: "InputDisplay", value: text, displayed: false)
}