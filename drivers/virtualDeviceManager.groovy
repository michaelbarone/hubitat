/**
 *  Virtual Device Manager
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
 * 	 10-12-20	mbarone			initial release 
 */
 
 
def setVersion(){
    state.name = "Virtual Device Manager"
	state.version = "0.0.1"
} 
 
metadata {
	definition (name: "Virtual Device Manager", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualDeviceManager.groovy") {
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "prependChildDeviceName", type: "text", title: "Prepend text to Child Device Names (this adds a dash (-) after the prepended text and will result in: textBelow-childDeviceName)", defaultValue: ""
	}

	attribute "Details","string"

	command "createVirutalDevice1", [[name:"Device Name*",type:"STRING"],
									[name:"Device Driver*",type:"ENUM",constraints:[
																			"Virtual Acceleration Sensor",
																			"Virtual audioVolume",
																			"Virtual Button",
																			"Virtual CO Detector",
																			"Virtual Color Temperature Light",
																			"Virtual Contact Sensor",
																			"Virtual Dimmer",
																			"Virtual Fan Controller",
																			"Virtual Garage Door Controller",
																			"Virtual Humidity Sensor",
																			"Virtual Illuminance Sensor"
																			]
									]
									]
									
	command "createVirutalDevice2", [[name:"Device Name*",type:"STRING"],
									[name:"Device Driver*",type:"ENUM",constraints:[
																			"Virtual Lock",
																			"Virtual Moisture Sensor",
																			"Virtual Motion Sensor",
																			"Virtual Multi Sensor",
																			"Virtual Omni Sensor",
																			"Virtual Presence",
																			"Virtual RGB Light",
																			"Virtual RGBW Light",
																			"Virtual Shade",
																			"Virtual Smoke Detector",
																			"Virtual Switch",
																			"Virtual Temperature Sensor",
																			"Virtual Thermostat"
																			]
									]
									]									

	command "createCustomVirtualDevice", [[name:"Device Name*",type:"STRING"],[name:"Device Driver*",type:"STRING"],[name:"Driver Namespace*",type:"STRING"]]
	
	command "removeChildDevice", [[name:"Device ID*",type:"STRING"]]
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
	updated()
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	log.degub "Removing all child devices"
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

private removeChildDevice(deviceID){
	def foundChildDevice = null
	foundChildDevice = getChildDevice("${deviceID}")

	if(foundChildDevice != null){
		deleteChildDevice("${deviceID}")
		sendEvent(name:"Details", value:"${deviceID} child device has been deleted.", displayed: true)
		unschedule(clearDetails)
		runIn(300,clearDetails)
	} else {
		sendEvent(name:"Details", value:"Could not find a device with ID (${deviceID}) to delete", displayed: true)
		unschedule(clearDetails)
		runIn(300,clearDetails)	
	}
}

def updated() {
	if(!state.customDrivers){
		state.customDrivers = [:]
	}
	clearDetails()
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

def createCustomVirtualDevice(deviceName,driver,namespace){
	createVirutalDevice(deviceName,driver,namespace)
}

def createVirutalDevice1(deviceName,driver){
	createVirutalDevice(deviceName,driver)
}

def createVirutalDevice2(deviceName,driver){
	createVirutalDevice(deviceName,driver)
}

def createVirutalDevice(deviceName,driver,namespace = "hubitat"){
    if (logEnable) log.debug "Creating Child Device "+deviceName
	
	def prepend = ""
	if(prependChildDeviceName && prependChildDeviceName!=""){
		prepend = prependChildDeviceName+"-"
	}
	
	foundChildDevice = null
	foundChildDevice = getChildDevice("${prepend}${deviceName}")

	if(foundChildDevice=="" || foundChildDevice==null){

		if (logEnable) log.debug "createChildDevice:  Creating Child Device '${prepend}${deviceName}'"
		try {
			addChildDevice(namespace, driver,
							"${prepend}${deviceName}",
							[
								completedSetup: true, 
								label: "${prepend}${deviceName}", 
								isComponent: false, 
								name: "${prepend}${deviceName}"
							]
						)
			if (logEnable) log.debug "createChildDevice: Child Device '${prepend}${deviceName}' created!"
			sendEvent(name:"Details", value:"${prepend}${deviceName} child device created!")
			if(namespace!="hubitat"){
				if(!state.customDrivers) {
					state.customDrivers = [:]
				}
				if(!state.customDrivers."${namespace}") {
					state.customDrivers."${namespace}" = []
				}
				if(!state.customDrivers."${namespace}".contains(driver)){
					state.customDrivers."${namespace}".add(driver)
				}
			}
			unschedule(clearDetails)
			runIn(300,clearDetails)
		}
		catch (e) {
			log.error "Child device creation failed with error = ${e}"
			sendEvent(name:"Details", value:"${prepend}${deviceName} child device creation failed. Please make sure that the '${driver}' is installed and published with the correct namespace: '${namespace}'.", displayed: true)
			unschedule(clearDetails)
			runIn(300,clearDetails)			
		}
	} else {
		if (logEnable) log.debug "createChildDevice: Child Device '${prepend}${deviceName}' found! Skipping"
		sendEvent(name:"Details", value:"${prepend}${deviceName} child device already exists.", displayed: true)
		unschedule(clearDetails)
		runIn(300,clearDetails)
	}
}