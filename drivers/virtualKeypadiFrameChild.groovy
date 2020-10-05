/**
 *  Virtual Keypad iFrame Child
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


def setVersion(){
    state.name = "Virtual Keypad iFrame Child"
	state.version = "0.0.1"
}

metadata {
    definition (name: "Virtual Keypad iFrame Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualKeypadiFrameChild.groovy") {
        capability "Switch"
        attribute "Keypad", "text"
    }
}

preferences {
    input("src", "text", title: "iFrame Url",  required: true)
}

def on() {
    sendEvent(name: "switch", value: "on")
	runIn(5,off)
    sendEvent(name: "Keypad", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
}

def off() {
    sendEvent(name: "switch", value: "off")
}

def installed() {
    on()
}

def updated() {
    on()
}