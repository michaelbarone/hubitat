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

// add built in polling so as to not need hubipoll
 

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
		attribute "weatherDescription","string"
		
		attribute "forecastHighDate","string"
		attribute "forecastHighTemp","number"
		attribute "forecastLowDate","string"
		attribute "forecastLowTemp","number"
		attribute "forecastNextRainDate","string"
		attribute "forecastNextRain","number"
	}

	preferences {
		input("dest_ip", "text", title: "Server IP", description: "The server with the dashboard app", required: true)
		input("dest_port", "number", title: "Server Port", description: "The port of the webserver (default 80)", required: true)
        input("dest_folder", "text", title: "Webserver script folder", description: "The subdirectory path for the main dashboard app", required: true)
		input("forceUpdate", "bool",title: "Force Update even if last update has not changed",defaultValue: false,required: false)
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,required: false)
	}
}


def parse(description) {
	unschedule(noServerResponse)
    logDebug "parse starting"
    def msg = parseLanMessage(description)
	//logDebug msg.json

	//logDebug msg.json.lastUpdate

	if(forceUpdate || state.lastUpdate < msg.json.lastUpdate) {
	
		state.lastUpdate = msg.json.lastUpdate
		
		def nowTime = new Date(msg.json.lastUpdate).format("MMM dd h:mm a", location.timeZone)
		sendEvent(name:"lastUpdate", value:nowTime, displayed: true)
		
		//logDebug msg.json.alerts.event
		//logDebug msg.json.alerts.headline
		
		def headline = "No Current Alert"
		def event = "No Current Alert"
		if(msg.json.alerts && msg.json.alerts.event && msg.json.alerts.headline){
			headline = msg.json.alerts.headline
			event = msg.json.alerts.event
		}
		
		sendEvent(name:"alertEvent", value:event, displayed: true)
		sendEvent(name:"alertHeadline", value:headline, displayed: true)
		

		
		//logDebug msg.json.current.temp
		//logDebug msg.json.current.rh
		sendEvent(name:"temperature", value:msg.json.current.temp, unit: "°F", displayed: true)
		sendEvent(name:"humidity", value:msg.json.current.rh, unit: "%", displayed: true)
		sendEvent(name:"weatherDescription", value:msg.json.current.weather.description, displayed: true)
		
		//logDebug msg.json.current.wind_spd
		sendEvent(name:"windSpeed", value:msg.json.current.wind_spd, unit: "mph", displayed: true)
		
		sendEvent(name:"dailyTempHigh", value:msg.json.daily[0].max_temp, unit: "°F", displayed: true)
		sendEvent(name:"dailyTempLow", value:msg.json.daily[0].low_temp, unit: "°F", displayed: true)
		
		
		def forecastHighDate,forecastLowDate,forecastNextRainDate
		def forecastHighTemp = 0
		def forecastLowTemp = 200
		def forecastNextRain = 0
		if(msg.json.daily.size > 0) {
			def thisCount = msg.json.daily.size - 1
			thisCount = thisCount.toInteger()
			(1..thisCount).each {
				if(msg.json.daily[it].high_temp > forecastHighTemp){
					forecastHighTemp = msg.json.daily[it].high_temp
					forecastHighDate = msg.json.daily[it].datetime
				}
				if(msg.json.daily[it].low_temp < forecastLowTemp){
					forecastLowTemp = msg.json.daily[it].low_temp
					forecastLowDate = msg.json.daily[it].datetime
				}
				if(msg.json.daily[it].precip > 0 && forecastNextRain == 0){
					forecastNextRainDate = msg.json.daily[it].datetime
					forecastNextRain = msg.json.daily[it].precip
				}
			}
			sendEvent(name:"forecastHighDate", value:forecastHighDate, displayed: true)
			sendEvent(name:"forecastHighTemp", value:forecastHighTemp, unit: "°F", displayed: true)
			sendEvent(name:"forecastLowDate", value:forecastLowDate, displayed: true)
			sendEvent(name:"forecastLowTemp", value:forecastLowTemp, unit: "°F", displayed: true)
			sendEvent(name:"forecastNextRainDate", value:forecastNextRainDate, displayed: true)
			sendEvent(name:"forecastNextRain", value:forecastNextRain, displayed: true)
		}

		clearDetails()
	} else {
		logDebug "No new data since lastUpdate"
	}
}

def updated() {
	clearDetails()
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
	if (forceUpdate) {
		log.warn "force update enabled...  auto disable in 30 minutes"
		runIn(1800,forceUpdateOff)
	}	
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def forceUpdateOff(){
    log.warn "force update disabled..."
    device.updateSetting("forceUpdate",[value:"false",type:"bool"])	
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

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}