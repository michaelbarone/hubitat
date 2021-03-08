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
	definition (name: "Ping Local Server Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/pingLocalServerChild.groovy") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "Health Check"

		attribute "lastUpdated", "String"
		attribute "timeAve", "Number"
		attribute "timeMin", "Number"
		attribute "timeMax", "Number"
		attribute "packetsSent", "Number"
		attribute "packetsLost", "Number"
    }

	preferences {
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,displayDuringSetup: true,required: false)
	}
}

def updated() {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def parse(String description) {
    logDebug "parse(${description}) called"
	def parts = description.split(" ")
    def name  = parts.length>0?parts[0].trim():null
    def value = parts.length>1?parts[1].trim():null
	logDebug "name: "+name+", value:"+value
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

def updateStats(stats){
	logDebug "$stats"
	def theseStats = stats.replaceAll("\\[","").replaceAll("\\]","")
	def parts = theseStats.split(", ")
	def theseParts
	parts.each {
		theseParts = it.split(":")
		if(["lost","sent"].contains(theseParts[0])){
			if(theseParts[0]=="lost"){
				sendEvent(name: "packetsLost", value: theseParts[1], displayed: false)
			} else {
				sendEvent(name: "packetsSent", value: theseParts[1], displayed: false)
			}
		} else {
			sendEvent(name: theseParts[0], value: theseParts[1], displayed: false)
		}
	}
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}

def installed() {
	updated()
}