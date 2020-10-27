/**
 *  Pull Weather Local Server
 *
 *  Copyright 2020 michaelbarone
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
 *   https://github.com/michaelbarone/homedashboard2
 *
 *   this app gets the weather.json that is created by the homedashboard webserver app and brings some data into hubitat child devices
 *
 *   you will need to setup polling for this device every x minutes to send the request to the webserver and handle the response, which will update the child devices.  this poll can be done through RM or similar or an app like "Simple Polling"
 *
 */


 

metadata {
	definition (name: "Pull Weather Local Server", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/pullWeatherLocalServer.groovy") {
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "TemperatureMeasurement"
		capability "RelativeHumidityMeasurement"
		
		
		attribute "Details","string"
		attribute "lastPoll","string"
		attribute "lastUpdate","string"
		
		attribute "alertHeadline","string"
		attribute "alertEvent","string"
		
		attribute "dailyTempHigh","number"
		attribute "dailyTempLow","number"
		attribute "windSpeed","number"

		command "removeChildren"
	}

	preferences {
		input("dest_ip", "text", title: "Server IP", description: "The server with the dashboard app", required: true)
		input("dest_port", "number", title: "Server Port", description: "The port of the webserver (default 80)", required: true)
        input("dest_folder", "text", title: "Webserver script folder", description: "The subdirectory path for the main dashboard app", required: true)
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,required: false)
	}
}


def parse(description) {
	unschedule(noServerResponse)
    logDebug "parse starting"
    def msg = parseLanMessage(description)
	//logDebug msg.json

	//logDebug msg.json.lastUpdate

	def headline = ""
	def event = ""

	if(state.lastUpdate < msg.json.lastUpdate) {
	
		state.lastUpdate = msg.json.lastUpdate
		
		def nowTime = new Date(msg.json.lastUpdate).format("MMM dd h:mm a", location.timeZone)
		sendEvent(name:"lastUpdate", value:nowTime, displayed: true)
		
		//logDebug msg.json.alerts.event
		//logDebug msg.json.alerts.headline
		
		if(msg.json.alerts.event && msg.json.alerts.headline){
			headline = msg.json.alerts.headline
			event = msg.json.alerts.event
		}
		
		sendEvent(name:"alertEvent", value:event, displayed: true)
		sendEvent(name:"alertHeadline", value:headline, displayed: true)
		

		
		//logDebug msg.json.current.temp
		//logDebug msg.json.current.rh
		sendEvent(name:"temperature", value:msg.json.current.temp, unit: "°F", displayed: true)
		sendEvent(name:"humidity", value:msg.json.current.rh, unit: "%", displayed: true)
		
		
		//logDebug msg.json.current.wind_spd
		sendEvent(name:"windSpeed", value:msg.json.current.wind_spd, unit: "mph", displayed: true)
		
		
		//logDebug msg.json.daily[0].high_temp
		//logDebug msg.json.daily[0].low_temp
		
		sendEvent(name:"dailyTempHigh", value:msg.json.daily[0].high_temp, unit: "°F", displayed: true)
		sendEvent(name:"dailyTempLow", value:msg.json.daily[0].low_temp, unit: "°F", displayed: true)
		clearDetails()
	} else {
		logDebug "No new data since lastUpdate"
	}
}
	
def refresh(){
	poll()
}

def poll() {
	def nowDay = new Date().format("MMM dd", location.timeZone)
	def nowTime = new Date().format("h:mm a", location.timeZone)
	sendEvent(name:"lastPoll", value:"Poll Started "+ nowDay + " at " + nowTime, displayed: true)
	logDebug "poll starting"

    def hubAction = new hubitat.device.HubAction(
   	 		'method': "GET",
    		'path': "/"+dest_folder+"/data/weather.json",
			'headers': [
					HOST: dest_ip+":"+dest_port
				]
		)
    logDebug "hubaction is: " + hubAction
	unschedule(noServerResponse)
	runIn(10,noServerResponse)
	sendHubCommand(hubAction)
}

def noServerResponse(){
	unschedule(noServerResponse)
	sendEvent(name:"Details", value:"Stale Data. No Response From Server.")
}

def updated() {
	clearDetails()
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}