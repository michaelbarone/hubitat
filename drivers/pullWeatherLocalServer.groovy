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
 *
 */


metadata {
	definition (name: "Pull Weather Local Server", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/pullWeatherLocalServer.groovy") {
		capability "Polling"
		capability "Refresh"
		capability "Initialize"
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
		attribute "uv","number"
		attribute "aqi","number"
		attribute "aqiIndex","string"
		attribute "dashTile","string"


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
		input("delayCheck", "number", title:"Number of seconds between pings", description: "", required: true, displayDuringSetup: true, defaultValue: "600")
		input("debugOutput", "bool",title: "Enable debug logging?",defaultValue: false,required: false)
	}
}


def parse(description) {
	unschedule(noServerResponse)
	unschedule(poll)
	runIn(delayCheck.toInteger(), poll)

    logDebug "parse starting"
    def msg = parseLanMessage(description)

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
		

		def uv = Math.round(msg.json.current.uv * 100) / 100
		sendEvent(name:"uv", value:uv, displayed: true)
		def uvIndex = "Good"
		def uvColor = "Green"
		Integer uvAdj = Math.round(uv * 100)
		logDebug uvAdj
		switch(uvAdj){
			case 0..300: uvIndex = "Good";uvColor = "Green"; break;
			case 301..600: uvIndex = "Moderate";uvColor = "Yellow"; break;
			case 601..800: uvIndex = "Unhealthy for Sensative Groups";uvColor = "Orange"; break;
			case 801..5000: uvIndex = "Unhealthy";uvColor = "Red"; break;
			default: uvIndex = "Good";uvColor = "Green"; break;
		}
		sendEvent(name:"uvIndex", value:uvIndex, displayed: true)


		Integer aqi = msg.json.current.aqi
		def aqiIndex = "Good"
		def aqiColor = "Green"
		sendEvent(name:"aqi", value:msg.json.current.aqi, displayed: true)
		switch(aqi){
			case 0..50: aqiIndex = "Good";aqiColor = "Green"; break;
			case 51..100: aqiIndex = "Moderate";aqiColor = "Yellow"; break;
			case 100..150: aqiIndex = "Unhealthy for Sensative Groups";aqiColor = "Orange"; break;
			case 151..200: aqiIndex = "Unhealthy";aqiColor = "Red"; break;
			case 201..300: aqiIndex = "Very Unhealthy";aqiColor = "dark-purple"; break;
			case 301..500: aqiIndex = "Hazardous";aqiColor = "rgb(98, 0, 2)"; break;
			default: aqiIndex = "Good";aqiColor = "Green"; break;
		}
		sendEvent(name:"aqiIndex", value:aqiIndex, displayed: true)
		

		def tileData = "<table width='100%'>"
		tileData += "<tr><td><div>AQI (0-500+):<br>${aqi} - <span style='color:${aqiColor};'>${aqiIndex}</span></div></td></tr>"
		tileData += "<tr><td><div>UV (0-11+):<br>${uv} - <span style='color:${uvColor};'>${uvIndex}</span></div></td></tr>"
		tileData += "</table>"

		sendEvent(name:"dashTile", value:tileData, displayed: true)

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
			(0..thisCount).each {
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
	unschedule(poll)
	runIn(5, poll)	
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void uninstalled(){
	unschedule()
}

def forceUpdateOff(){
    log.warn "force update disabled..."
    device.updateSetting("forceUpdate",[value:"false",type:"bool"])	
}
	
void initialize(){
	unschedule(poll)
    runIn(10, poll)
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

    def hubAction = new hubitat.device.HubAction(
   	 		'method': "GET",
    		'path': "/"+dest_folder+"/data/weather.json",
			'headers': [
					HOST: dest_ip+":"+dest_port
				]
		)
    logDebug "hubaction is: " + hubAction
	unschedule(noServerResponse)
	runIn(15,noServerResponse)
	sendHubCommand(hubAction)
}

def noServerResponse(){
	unschedule(noServerResponse)
	sendEvent(name:"Details", value:"Stale Data. No Response From Server.")
	unschedule(poll)
	runIn(delayCheck.toInteger(), poll)	
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}