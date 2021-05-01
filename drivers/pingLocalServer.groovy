/**
 *  HTTP Ping Local Server
 *
 *  Copyright 2014 Kristopher Kubicki
 *  updated by michaelbarone 2018
 *  https://github.com/michaelbarone/smartThings/tree/master/deviceHandlers
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
 *
 *   This handler uses a local webserver to perform the local pings and handle the responses.  here is an example that uses php, but this can be written in any web language that can handle the requirements.
 *
 *   https://github.com/michaelbarone/ping
 *
 *   The web input the script is expecting:   webserver/ping.php?ips=192.168.3.217  or for multiple machines to ping:  webserver/ping.php?ips=192.168.3.217,192.168.3.221
 *   the required response for this handler is:
 *      for 1 ip:  [{"stats":{"sent":"2","lost":0,"timeMax":0,"timeAve":0},"lastUpdate":1600110399,"ip":"192.168.3.217","status":"on"}]
 *      for multiple ips:  [{"stats":{"sent":"2","lost":0,"timeMax":0,"timeAve":0},"lastUpdate":1600110413,"ip":"192.168.3.217","status":"on"},{"stats":{"sent":"2","lost":0,"timeMax":0,"timeAve":0},"lastUpdate":1600110414,"ip":"192.168.3.221","status":"on"}]
 *
 *
 *
 */


 

metadata {
	definition (name: "Ping Local Server", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/pingLocalServer.groovy") {
		capability "Polling"
		capability "Refresh"
		capability "Initialize"
		capability "Sensor"
		
		attribute "Details","string"
		attribute "lastPoll","string"

		command "removeChildren"
	}

	preferences {
		input("dest_ip", "text", title: "Server IP", description: "The server with ping.php", required: true, displayDuringSetup: true)
		input("dest_port", "number", title: "Server Port", description: "The port of the webserver (default 80)", required: true, displayDuringSetup: true)
        input("dest_folder", "text", title: "Webserver script folder", description: "The subdirectory path for the ping script", required: true, displayDuringSetup: true)
		input("ip_addresses", "text", title: "IP Addresses", description: "IP for each machine you want status updates for.  Comma separated if multiple IPs are set.  IE:  192.168.1.50,192.168.1.20", required: true, displayDuringSetup: true)
		input("delayCheck", "number", title:"Number of seconds between pings", description: "", required: true, displayDuringSetup: true, defaultValue: "300")
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,displayDuringSetup: true,required: false)
	}
}


def parse(description) {
	unschedule(poll)
	runIn(delayCheck.toInteger(), poll)
    logDebug "parse starting"
    def msg = parseLanMessage(description)
	logDebug msg.json
	//def cmds = []
	//cmds << "delay 1000"
	
	msg.json.each {
		logDebug "item: $it.ip is $it.status"
		changeChildValue(it.ip, it.status, it.stats)
		//cmds
	}
}

void initialize(){
	unschedule(poll)
    runIn(10, poll)
}

void uninstalled(){
	unschedule()
}

def refresh(){
	unschedule(poll)
    runIn(1, poll)
}

def poll() {
	def nowDay = new Date().format("MMM dd", location.timeZone)
	def nowTime = new Date().format("h:mm a", location.timeZone)
	sendEvent(name:"lastPoll", value:"Poll Started "+ nowDay + " at " + nowTime, displayed: true)
	logDebug "poll starting"
    def hosthex = convertIPtoHex(dest_ip)
    def porthex = convertPortToHex(dest_port)
    if (porthex.length() < 4) { porthex = "00" + porthex }
    device.deviceNetworkId = "$hosthex:$porthex" 
	logDebug "device.deviceNetworkId is " + device.deviceNetworkId
    
    def hubAction = new hubitat.device.HubAction(
   	 		'method': "GET",
    		'path': "/"+dest_folder+"/?ips="+ip_addresses,
			'headers': [
					HOST: dest_ip+":"+dest_port
				]
		)
    logDebug "hubaction is: " + hubAction
    
    //hubAction
	sendHubCommand(hubAction)
}

def updated() {
	clearDetails()
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
	unschedule(poll)
	runIn(delayCheck.toInteger(), poll)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}	


 private void changeChildValue(title, param, stats) {
	logDebug "changeChildValue: "+title+" is "+param
	def childDevice = null
	def name = title
	def value = param
	def deviceType = "switch"
	logDebug "${device.deviceNetworkId}-${name}"
	try {
		if(childDevices){
			childDevices.each {
				try{
					if (it.deviceNetworkId == "${device.deviceNetworkId}-${name}") {
						childDevice = it
					}
				}
				catch (e) {
					logDebug e
				}
			}
		}
	}
	catch (e) {
        log.error "Error in changeChildValue() routine1, error = ${e}"
	}
	try {
		if (childDevice == null) {
			logDebug "isChild = true, but no child found - Auto Add it!"
			logDebug "    Need a ${name}"
		
			createChildDevice(name)
			//find child again, since it should now exist!
			childDevices.each {
				try{
					if (it.deviceNetworkId == "${device.deviceNetworkId}-${name}") {
						childDevice = it
					}
				}
				catch (e) {
					logDebug e
				}
			}
		}
		if (childDevice != null) {
            childDevice.parse("${deviceType} ${value}")
			childDevice.updateStats("${stats}")
		}
	}
	catch (e) {
        log.error "Error in changeChildValue() routine2, error = ${e}"
	}
 }
 
 private void createChildDevice(String deviceName) {
	logDebug "createChildDevice:  Creating Child Device '${device.displayName} (${deviceName})'"
	try {
		def deviceHandlerName = "Ping Local Server Child"
		addChildDevice(deviceHandlerName,
						"${device.deviceNetworkId}-${deviceName}",
						[
							completedSetup: true, 
							label: "${device.displayName} (${deviceName})", 
							isComponent: true, 
							componentName: "${deviceName}", 
							componentLabel: "${deviceName}"
						]
					)
		unschedule(clearDetails)
		runIn(300,clearDetails)
        sendEvent(name:"Details", value:"Child device created!  May take some time to display.")
	}
	catch (e) {
        log.error "Child device creation failed with error = ${e}"
		unschedule(clearDetails)
		runIn(300,clearDetails)		
        sendEvent(name:"Details", value:"Child device creation failed. Please make sure that the '${deviceHandlerName}' is installed and published.", displayed: true)
	}
 }
 def removeChildren(){
	logDebug "removing any child devices"
	childDevices.each {
		try{
			logDebug "removing ${it.deviceNetworkId}"
			deleteChildDevice(it.deviceNetworkId)
		}
		catch (e) {
			logDebug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
	unschedule(clearDetails)
	runIn(300,clearDetails)
	sendEvent(name:"Details", value:"Child devices removed!  Refresh/Poll when ready to re-build child devices, or wait until a device changes state.")
 }

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}

