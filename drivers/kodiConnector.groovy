/**
 *  Kodi Connector
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
 *  To use the video camera view, you must install and configure this plugin for KODI:
 *  https://github.com/michaelbarone/script.securitycam
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 * 	 9-26-20	mbarone			initial release 
 */
 
 
def setVersion(){
    state.name = "Kodi Connector"
	state.version = "0.0.1"
} 
 
metadata {
	definition (name: "Kodi Connector", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/kodiConnector.groovy") {
		capability "Notification"
		capability "Actuator"
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}

	attribute "Details","string"

	command "createChild", [[name:"KODI Name*",type:"STRING"]]
	command "viewMotionCamera", [[name:"cameraID*",type:"NUMBER"]]
	command "viewAllCameras"
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
	updated()
}

def updated() {
	clearDetails()
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

def deviceNotification(message){
	childDevices.each {
		try{
			it?.deviceNotification(message)
		}
		catch (e) {
			if (logEnable) log.debug "Error sending deviceNotification to childDevice ${it.deviceNetworkId}: ${e}"
		}
	}
}

def viewMotionCamera(cameraID){
	childDevices.each {
		try{
			it?.viewMotionCamera(cameraID)
		}
		catch (e) {
			if (logEnable) log.debug "Error sending viewMotionCamera to childDevice ${it.deviceNetworkId}: ${e}"
		}
	}
}

def viewAllCameras(){
	childDevices.each {
		try{
			it?.viewAllCameras()
		}
		catch (e) {
			if (logEnable) log.debug "Error sending viewAllCameras to childDevice ${it.deviceNetworkId}: ${e}"
		}
	}
}

def createChild(kodi){
    if (logEnable) log.debug "Creating Child Device "+kodi
	
	foundChildDevice = null
	foundChildDevice = getChildDevice("${device.deviceNetworkId}-${kodi}")

	if(foundChildDevice=="" || foundChildDevice==null){

		if (logEnable) log.debug "createChildDevice:  Creating Child Device '${device.displayName} (${kodi})'"
		try {
			def deviceHandlerName = "Kodi Connector Child"
			addChildDevice(deviceHandlerName,
							"${device.deviceNetworkId}-${kodi}",
							[
								completedSetup: true, 
								label: "${device.displayName} (${kodi})", 
								isComponent: false, 
								name: "${device.displayName} (${kodi})",
							]
						)
			sendEvent(name:"Details", value:"Child device created!  May take some time to display.")
			unschedule(clearDetails)
			runIn(300,clearDetails)
		}
		catch (e) {
			log.error "Child device creation failed with error = ${e}"
			sendEvent(name:"Details", value:"Child device creation failed. Please make sure that the '${deviceHandlerName}' is installed and published.", displayed: true)
		}
	} else {
		if (logEnable) log.debug "createChildDevice: Child Device '${device.displayName} (${it})' found! Skipping"
	}
}