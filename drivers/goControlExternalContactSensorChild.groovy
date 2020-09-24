/**
 *  GoControl External Contact Sensor v1.1
 *  (Child Device Handler for WADWAZ-1)
 *
 *  Author: 
 *    Kevin LaFramboise (krlaframboise)
 *
 *  URL to documentation:
 *    
 *  Changelog:
 *
 *    1.1 (07/01/2017)
 *      -  Updated colors to match SmartThing's new color theme.
 *
 *    1.0 (04/08/2017)
 *      -  Initial Release
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
 */
metadata {
	definition (
		name: "GoControl External Contact Sensor Child", 
		namespace: "mbarone", 
		author: "Kevin LaFramboise  - updated by mbarone",
		importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/goControlExternalContactSensorChild.groovy"
	) {
		capability "Sensor"
		capability "Contact Sensor"
        capability "Health Check"
        
		attribute "primaryStatus", "string"
		attribute "lastCheckin", "string"
		attribute "lastOpen", "string"
		attribute "lastClosed", "string"
		
		//command "setOpen"
		//command "setClose"
		//command "open"
		//command "close"
	}

	preferences {
		input "primaryStatusAttr", "enum",
			title: "Primary Status Attribute:",
			defaultValue: primaryStatusAttrSetting,
			required: false,
			options: primaryStatusOptions
		input "contactOpen", "enum",
			title: "Contact Open Event:",
			defaultValue: contactOpenSetting,
			required: false,
			options: eventOptions
		input "contactClosed", "enum",
			title: "Contact Closed Event:",
			defaultValue: contactClosedSetting,
			required: false,
			options: eventOptions
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: debugOutputSetting, 
			displayDuringSetup: true, 
			required: false			
	}
}

def setOpen() {
	handleContactEvent("open")
	def nowDay = new Date().format("MM/dd/yyyy", location.timeZone)
	def nowTime = new Date().format("hh:mm:ss a", location.timeZone)
	sendEvent(name: "lastOpen", value: nowDay + " " + nowTime, displayed: false)
    //parent?.openExternal()
}

def setClose() {
	handleContactEvent("closed")
	def nowDay = new Date().format("MM/dd/yyyy", location.timeZone)
	def nowTime = new Date().format("hh:mm:ss a", location.timeZone)
	sendEvent(name: "lastClosed", value: nowDay + " " + nowTime, displayed: false)
    //parent?.closeExternal()
}

def open(){
    setOpen()
}

def close(){
    setClose()
}

def parse(String description) {		
	return []
}

def updated() {	
	log.trace "updated"
	if (!isDuplicateCommand(state.lastUpdated, 500)) {
		state.lastUpdated = new Date().time

		if (!state.isConfigured) {		
			configure()
		}
		
		handleContactEvent(state.sensorVal, false)	
	}
}

def configure() {	
	logTrace "configure()"
	
	state.sensorVal = "closed"
	
	eventSettings.each {	
		if (!device.currentValue("${it.attr}")) {
			def val = (it.aEvent == "default" ? it.aVal : it.iVal)
			sendEvent(name: "${it.attr}", value: "$val", isStateChange: true, displayed: false)
		}
	}
	state.isConfigured = true	
}

private void handleContactEvent(sensorVal, displayed=null) {
	def eventMaps = []
	
	state.sensorVal = "${sensorVal}"
	
	eventSettings.each {
		def eventVal = determineEventVal(it, sensorVal)
        logDebug eventVal
		if (eventVal) {
			
			eventMaps += createEventMap(it.attr, eventVal, displayed)
			
			if (primaryStatusAttrSetting.startsWith(it.attr)) {				
				eventMaps += createEventMap("primaryStatus", (
				primaryStatusAttrSetting.contains("garage") ? "garage-${eventVal}" : eventVal), false)
			}
		}		
	}
	eventMaps?.each { 
		sendEvent(it) 
	}
}

private determineEventVal(eventSettings, sensorVal) {
	if (eventSettingMatchesEventVal(eventSettings.aEvent, sensorVal)) {
		return eventSettings.aVal
	}
	else if (eventSettingMatchesEventVal(eventSettings.iEvent, sensorVal)) {
		return eventSettings.iVal
	}
	else {
		return null
	}
}

private eventSettingMatchesEventVal(eventSetting, sensorVal) {
	return (eventSetting == "default" || eventSetting?.endsWith(sensorVal))	
}

private createEventMap(eventName, newVal, displayed=null) {	
	def isNew = device.currentValue(eventName) != newVal
	def desc = "${eventName.capitalize()} is ${newVal}"
	
	def result = []
	if (isNew) {
		logDebug "${desc}"
		result << [
			name: eventName, 
			value: newVal, 
			displayed: (displayed != null) ? displayed : isNew
		]
	}
	else {
		logTrace "Ignored: ${desc}"
	}
	return result
}

// Settings
private getEventSettings() {
	return [
		[attr: "contact", aEvent: contactOpenSetting, aVal: "open", iEvent: contactClosedSetting, iVal: "closed"]
	]
}

private getPrimaryStatusAttrSetting() {
	return settings?.primaryStatusAttr ?: "contact"
}

private getContactOpenSetting() {
	return settings?.contactOpen ?: "contact.open"
}

private getContactClosedSetting() {
	return settings?.contactClosed ?: "default"
}

private getDebugOutputSetting() {
	return settings?.debugOutput || settings?.debugOutput == null
}

// Options 
private getEventOptions() {
	return [
		"default",
		"none", 
		"contact.open",
		"contact.closed"
	]
}

private getPrimaryStatusOptions() {
	return [
		"contact",
		"contact-garage"
	]
}

private isDuplicateCommand(lastExecuted, allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time) 
}

private logDebug(msg) {
	if (debugOutputSetting) {
		log.debug "$msg"
	}
}

private logTrace(msg) {
	// log.trace "$msg"
}