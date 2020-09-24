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
	importUrl: "",
)

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {	
    if(logEnable) log.debug "Updated with settings: ${settings}"
	
	// send settings to child app
	
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
    setDefaults()
	
	/*
	if(getChildDevice(dataName)) {
		subscribe(dataName, "switch", keypadSwitch)
		subscribe(dataName, "button", keypadButton)
	}
	*/
	
	/*
    if(contactSensors) subscribe(contactSensors, "contact", contactGroupHandler)
    if(locks) subscribe(locks, "lock", lockGroupHandler)
	if(motionSensors) subscribe(motionSensors, "motion", motionGroupHandler)
    if(switches) subscribe(switches, "switch", switchGroupHandler)
    if(waterSensors) subscribe(waterSensors, "water", waterGroupHandler)
	*/
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

/*
def keypadSwitch(evt) {
	if(logEnable) log.debug "keypadSwitch: ${evt}"
}

def keypadButton(evt) {
	if(logEnable) log.debug "keypadButton: ${evt}"
}
*/

def setMode(mode){
	if(logEnable) log.debug "setMode: ${mode}"
	setLocationMode('Disarmed')
}


preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
		display() 
        section("<b>Instructions:</b>", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
    		paragraph "Create a custom keypad!"
		}
        section(getFormat("header-green", "Virtual Keypad Device Creation")) {
            paragraph "Do not change the name of the keypad once fully configured"
		    input "dataName", "text", title: "Enter a name for this vitual Device (ie. 'Keypad - Main')", required:true, submitOnChange:true
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
					title: "Delay before setting HSM arm commands. Default: Off/false"
				if (armDelay){
					input "armDelaySeconds", "number", required: armDelay, range: "10..90", defaultValue: 30,
						title: "Number of seconds before HSM arm commands are executed. Default: 30, range 10-90"
				}
			}

			section(getFormat("header-green", "Virtual Keypad Mode Options")) {
				input "changeModes", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Change select modes from keypad. Default: Off/false"
				if (changeModes){
					input "availableModes", "mode", required: changeModes, multiple: true,
						title: "Allow these modes to be set from keypad"
				}
			}
			
			section(getFormat("header-green", "Virtual Keypad HSM Options")) {
				input "customHSM", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Toggle custom triggers for RM or other apps to run logic. Default: Off/false"
				if (customHSM){
					input "availableCustomHSM", "enum", required: customHSM, multiple: true, options: ["Arm","ReArm","Disarm"],
						title: "Allow these generic HSM triggers to be toggled from keypad"
				}				
			
				input "changeHSM", "bool", required: false, defaultValue: false, submitOnChange: true,
					title: "Change select HSM modes from keypad. Default: Off/false"
				if (changeHSM){
					input "availableHSM", "enum", required: changeHSM, multiple: true, options: ["armAway", "armHome", "armNight", "disarm", "armRules", "disarmRules", "disarmAll", "armAll", "cancelAlerts"],
						title: "Allow these HSM modes to be set from keypad"
				}
			}			

			section(getFormat("header-green", "Virtual Keypad Lock Codes")) {
				paragraph "Set lock codes in the keypad device or using an app like Lock Code Manager"
			
				//if(theChildDevice.getLockCodes()){
				//	paragraph "${theChildDevice.getLockCodes()}"
				//}
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
        //paragraph "${state.headerMessage}"
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