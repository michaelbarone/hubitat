/**
 *  Virtual Keypad Child
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
 * 
 */
 
 def setVersion(){
    state.name = "Virtual Keypad Child"
	state.version = "0.0.1"
}

definition(
    name: "Virtual Keypad Child",
    namespace: "mbarone",
    author: "mbarone",
    description: "An Individual Virutal Keypad",
    category: "Convenience",
	parent: "mbarone:Virtual Keypad Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/apps/virtualKeypadChild.groovy",
)

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {	
    if(logEnable) log.debug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
	def theChild = getChildDevice(settings.dataName)
	if(theChild) {
		if(logEnable) log.debug "Child Keypad Found, applying settings and creating keypad children if needed"
		theChild.configureSettings(settings)
	}
    setDefaults()
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def setMode(mode){
	if(logEnable) log.debug "setMode: ${mode}"
	// check if mode in available modes
	if(location.modes.any{mode.toString().contains(it.toString())}){
		if(logEnable) log.debug "mode available and setting: ${mode}"
		setLocationMode(mode)
	}
}

def notify(text){
	notifyDevices.each{
		it.deviceNotification(text)
	}
}

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
		display()

		def theModes = location.modes.clone()
		theModes = theModes.collect { "Mode-$it" }
		def HSM = ["armAway", "armHome", "armNight", "disarm", "armRules", "disarmRules", "disarmAll", "armAll", "cancelAlerts"]
		HSM = HSM.collect { "HSM-$it" }
		theModes.addAll(HSM)
		theModes.addAll(["Custom-Arm","Custom-ReArm","Custom-Disarm"])
		
        section("<b>Instructions:</b>", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
    		paragraph "Create a custom keypad!"
		}
        section(getFormat("header-green", "Virtual Keypad Device Creation")) {
            paragraph "Do not change the name of the keypad once fully configured"
		    input "dataName", "text", title: "Enter a name for this vitual Device (ie. 'Keypad - Main' or 'Keypad - Guest')", required:true, submitOnChange:true
 		    paragraph "<b>A device will automaticaly be created for you as soon as you click outside of this field.</b>"
		    if(dataName) createKeypadChildDevice()
			if(statusMessageD == null) statusMessageD = "Waiting on status message..."
			paragraph "${statusMessageD}"
        }
        
		if(getChildDevice(dataName)) {
			def theChildDevice = getChildDevice(dataName)
			section(getFormat("header-green", "Virtual Keypad Device Settings")) {
				paragraph "Configure your keypad options"
				input "armDelay", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Delay before executing button commands. Default: Off/false"
				if (armDelay){
					input "armDelaySeconds", "number", required: armDelay, range: "10..90", defaultValue: 30,
						title: "Number of seconds before button commands are executed. Default: 30, range 10-90"

					input "armDelaySecondsGroup", "enum", required: armDelay, multiple: true, options: theModes,
						title: "What commands do you want to delay before executing?"
				}
				
				paragraph ""
				paragraph ""
				paragraph "Select commands you do NOT require a code to execute:"
				input "noCodeRequired", "enum", required: false, multiple: true, options: theModes,
					title: "These commands need NO code to execute"
				
			}
			
			section(getFormat("header-green", "Virtual Keypad External Triggers")) {
				paragraph "Each Mode and HSM button that is created also has a switch that can be used to trigger RM or other automations when that button is executed.  These switches will always turn on (and auto off) regardless of the below Mode and HSM options."
			
				paragraph "There are 3 built in Custom triggers that you can use to trigger RM or other custom automations.  These do not change modes or HSM directly, only turn on their individual switch when triggered:"
				paragraph "Custom-Arm - turns on the Custom-Arm switch"
				paragraph "Custom-Disarm - turns on the Custom-Disarm switch"
				paragraph "Custom-ReArm - turns on the Custom-Disarm switch, then turns on the Custom-Arm switch (after a delay if this option is enabled for Custom-ReArm)"
			}

			section(getFormat("header-green", "Virtual Keypad Mode Options")) {
				input "changeModes", "bool", required: true, defaultValue: true, submitOnChange: true,
					title: "Have the Keypad app change modes directly. Default: On/true"
					
				if(changeModes){
					paragraph "change to specific mode before executing final mode change"
				
					input "defaultMode", "mode", required: false, multiple: false, submitOnChange: true,
						title: "Change to this mode before executing the below modes"	
					
					input "defaultModeTrigger", "mode", required: defaultMode, multiple: true,
						title: "These modes will execute after switching to the above mode (after delay if set)"
				}
			}
			
			section(getFormat("header-green", "Virtual Keypad HSM Options")) {
				/*
				input "customHSM", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Toggle custom triggers for RM or other apps to run logic. Default: Off/false"
				if (customHSM){
					input "availableCustomHSM", "enum", required: customHSM, multiple: true, options: ["Arm","ReArm","Disarm"],
						title: "Allow these generic HSM triggers to be toggled from keypad"
				}
				*/
			
				input "changeHSM", "bool", required: true, defaultValue: true, submitOnChange: true,
					title: "Have the Keypad app change HSM directly. Default: On/true"
			}


			section(getFormat("header-green", "Virtual Keypad Lock Codes")) {
				paragraph "Set lock codes in the keypad device or using an app like Lock Code Manager"
			}
			
			section(getFormat("header-green", "Notifications on Bad Code Entry")) {
				input "notify", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Notify you if a bad code has been entered. Default: Off/false"
				if (notify){
					input "notifyDevices", "capability.notification", required: notify, multiple: true,
						title: "Choose your notification devices"
						
					input "notifyLimit", "number", required: notify, defaultValue: 1,
						title: "Send notification after this many bad codes"					
				}
			}			

		}
		
        section(getFormat("header-green", "Maintenance")) {
            label title: "Enter a name for this keypad app child", required:false, submitOnChange:true
            input "logEnable","bool", title: "Enable Debug Logging", description: "Debugging", defaultValue: false, submitOnChange: true
		}
		display2()
	}
}

def display() {
    setVersion()
    theName = app.label
    if(theName == null || theName == "") theName = "New Child App"
    section (getFormat("title", "${state.name} - ${theName}")) {
		paragraph getFormat("line")
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>${state.name} - ${state.version}</div>"
	}       
}




def createKeypadChildDevice() {
    if(logEnable) log.debug "In createKeypadChildDevice (${state.version})"
    statusMessageD = ""
    if(!getChildDevice(dataName)) {
        if(logEnable) log.debug "In createKeypadChildDevice - Child device not found - Creating device: ${dataName}"
        try {
            addChildDevice("mbarone", "Virtual Keypad", dataName, 1234, ["name": "${dataName}", isComponent: false])
            if(logEnable) log.debug "In createKeypadChildDevice - Child device has been created! (${dataName})"
            statusMessageD = "<b>Device has been been created. (${dataName})</b>"
        } catch (e) { if(logEnable) log.debug "Simple Groups unable to create device - ${e}" }
    } else {
        statusMessageD = "<b>Device Name (${dataName}) already exists.</b>"
    }
    return statusMessageD
}

def setDefaults() {
	if(logEnable == null){logEnable = true}
}

def getFormat(type, myText="") {			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}