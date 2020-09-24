/**
 *  Ping Local Server Child
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
 *
 * 
 */
metadata {
	definition (name: "Ping Local Server Child", namespace: "mbarone", author: "mbarone") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "Health Check"

		attribute "lastUpdated", "String"
    }

	preferences {
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,displayDuringSetup: true,required: false)
	}
}

def parse(String description) {
    logDebug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
    if (name && value) {
    	logDebug "name: "+name+", value:"+value
        // Update device
        sendEvent(name: name, value: value)
        // Update lastUpdated date and time
        def nowDay = new Date().format("MMM dd", location.timeZone)
        def nowTime = new Date().format("h:mm a", location.timeZone)
        sendEvent(name: "lastUpdated", value: nowDay + " at " + nowTime, displayed: false)
    }
    else {
    	logDebug "Missing either name or value.  Cannot parse!"
    }
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}

def installed() {
}