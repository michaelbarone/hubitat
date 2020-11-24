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
 * 	 9-26-20	mbarone			initial release 
 * 	 10-01-20	mbarone			added panic button
 * 	 10-08-20	mbarone			command button customization.  choose the command buttons you want child devices created for
 * 	 10-16-20	mbarone			added user defined custom commands (adjusted version down to 0.0.4 to match change history
 * 	 11-18-20	mbarone			added selected commands can cancel HSM alerts when triggered
 */
 
 def setVersion(){
    state.name = "Virtual Keypad Child"
	state.version = "0.0.5"
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
	updated()
}

def updated() {
	unschedule()
    unsubscribe()
	initialize()
	// also defined in pageConfig block
	app.updateLabel(label)
	def buttonsCustom = ["Custom-Arm","Custom-ReArm","Custom-Disarm"]
	def availableButtons = updateButtonOptions(buttonsCustom,buttonsHSMIncluded,buttonsModesIncluded)
	settings.put("availableButtons",availableButtons)
	def theChild = getChildDevice(settings.dataName)
	if(theChild) {
		if(logEnable) log.debug "Child Keypad Found, applying settings and creating keypad children if needed"
		theChild.configureSettings(settings)
	}
    if(logEnable) log.debug "Updated with settings: ${settings}"
}

def initialize() {
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

		def buttonsModes = location.modes.clone()
		buttonsModes = buttonsModes.collect { "Mode-$it" }
		def buttonsHSM = ["armAway", "armHome", "armNight", "disarm", "armRules", "disarmRules", "disarmAll", "armAll", "cancelAlerts"]
		buttonsHSM = buttonsHSM.collect { "HSM-$it" }
		// also defined in updated block
		def buttonsCustom = ["Custom-Arm","Custom-ReArm","Custom-Disarm"]

		if(!state.customCommands){
			state.customCommands = []
		}
		
        section("<b>Instructions:</b>", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
    		paragraph "Create a custom keypad!"
		}
		
		section(getFormat("header-green", "Virtual Keypad Device Creation")) {
			if(!dataName) {
				input "dataName", "text", title: "Enter a name for this vitual Device (ie. 'Keypad - Main' or 'Keypad - Guest')", required:true, submitOnChange:true
				paragraph "<b>A device will automaticaly be created for you as soon as you click outside of this field.</b>"
			}
			if(dataName) {
				def statusMessageD = createKeypadChildDevice()
			}
			if(statusMessageD == null) statusMessageD = "Waiting on status message..."
			paragraph "${statusMessageD}"
		}
        
		if(getChildDevice(dataName)) {
			def theChildDevice = getChildDevice(dataName)
			
			section(getFormat("header-green", "Virtual Keypad External Triggers")) {
				paragraph "Each Mode and HSM button that is created also has a switch that can be used to trigger RM or other automations when that button is executed.  These switches will always turn on (and auto off) regardless of the below Mode and HSM options."
			
				paragraph "There are 3 built in Custom triggers that you can use to trigger RM or other custom automations.  These do not change modes or HSM directly, only turn on their individual switch when triggered:"
				paragraph "Custom-Arm - turns on the Custom-Arm switch"
				paragraph "Custom-Disarm - turns on the Custom-Disarm switch"
				paragraph "Custom-ReArm - turns on the Custom-Disarm switch, then turns on the Custom-Arm switch (after a delay if this option is enabled for Custom-ReArm)"
				
				input "includeButtonsCustom", "bool", required: true, defaultValue: false, submitOnChange: true,
					title: "Create the above built in custom button child devices."

				paragraph "**Changing this may require you to update the tile devices for your Keypad dashboard**"
				
				paragraph ""
				paragraph ""
				paragraph ""

			
				paragraph "You can create custom command triggers below.  These can be for 'GarageDoor' or any RM/automation you want to start from the keypad."
				input "customCommand", "text", title: "Enter a name for this custom command (ie. 'GarageDoor' or 'ReleaseTheHounds'), do not use spaces or dashes in names (' ' or '-')", required:false, submitOnChange:true

				if(customCommand){
					state.customCommands << "Custom-${customCommand}"
					app.clearSetting("customCommand")
				}
				
				state.customCommands.each{
					input "RemoveCustomCommand--${it}", "button", title: "${it} (click to remove)"
				}
			}

			section(getFormat("header-green", "Virtual Keypad Mode Options")) {
				input "changeModes", "bool", required: true, defaultValue: false, submitOnChange: true,
					title: "Have the Keypad app change modes directly."

				if(changeModes){
					
					input "buttonsModesIncluded", "enum", required: changeModes, multiple: true, options: buttonsModes, defaultValue: buttonsModes, submitOnChange: true,
						title: "Create the Mode button child devices"				
				
				
					paragraph "Only Modes selected above will be available on the Keypad.  **Changing this may require you to update the tile devices for your Keypad dashboard**"
					
					/*  // Limited to buttonsModesIncluded choices
					input "defaultMode", "enum", required: false, multiple: false, options: buttonsModesIncluded,
						title: "Change to this mode before executing the below modes"
					*/

					// all modes available for default, do not need to create a child device for this function
					input "defaultMode", "mode", required: false, multiple: false, submitOnChange: true,
						title: "Change to this mode before executing the below modes"
					
					
					input "defaultModeTrigger", "enum", required: false, multiple: true, options: buttonsModesIncluded,
						title: "These modes will execute after switching to the above mode (after delay if set)"						

				}
			}
			
			section(getFormat("header-green", "Virtual Keypad HSM Options")) {
				input "changeHSM", "bool", required: true, defaultValue: false, submitOnChange: true,
					title: "Have the Keypad app change HSM directly."

				if(changeHSM){
					
					input "buttonsHSMIncluded", "enum", required: changeHSM, multiple: true, options: buttonsHSM, defaultValue: buttonsHSM, submitOnChange: true,
						title: "Create the HSM button child devices"
				
				
					paragraph "Only HSM options selected above will be available on the Keypad.  **Changing this may require you to update the tile devices for your Keypad dashboard**"
				}					
					
			}

			section(getFormat("header-green", "Virtual Keypad Command Button Settings")) {
			
				if(!changeHSM && !changeModes && !includeButtonsCustom && state.customCommands==[]){
					paragraph "<b>** You must select some commands for child devices above **</b>"
				} else {
				
					paragraph "Configure your keypad options"
					input "armDelay", "bool", required: false, defaultValue: false, submitOnChange: true,
						title: "Delay before executing button commands. Default: Off/false"
					if (armDelay){
						input "armDelaySeconds", "number", required: armDelay, range: "10..90", defaultValue: 30,
							title: "Number of seconds before button commands are executed. Default: 30, range 10-90"

						input "armDelaySecondsGroup", "enum", required: armDelay, multiple: true, options: updateButtonOptions(buttonsCustom,buttonsHSMIncluded,buttonsModesIncluded),
							title: "What commands do you want to delay before executing?"
					}
					
					paragraph ""
					paragraph ""
					paragraph "Select commands you do NOT require a code to execute:"
					input "noCodeRequired", "enum", required: false, multiple: true, options: updateButtonOptions(buttonsCustom,buttonsHSMIncluded,buttonsModesIncluded),
						title: "These commands need NO code to execute"

					paragraph ""
					paragraph ""
					input "cancelAlertsOnDisarm", "bool", required: true, defaultValue: true, submitOnChange: true,
						title: "When 'On', HSM alerts will be cancelled when any of the disarm commands are used. Default: On/true"
						paragraph "Disarm Commands: Custom-Disarm, HSM-disarm, HSM-disarmAll, HSM-disarmRules, any mode or custom command with 'disarm' in the name"
					
					paragraph ""
					input "cancelAlertsOnCommands", "enum", required: false, multiple: true, options: updateButtonOptions(buttonsCustom,buttonsHSMIncluded,buttonsModesIncluded),
							title: "Specify additional commands that cancel HSM alerts that do not have 'disarm' in the name"
				}
				
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
			input "label", "string", title: "Enter a name for this keypad app child", required:false, submitOnChange:true, defaultValue:dataName
            input "logEnable","bool", title: "Enable Debug Logging", description: "Debugging", defaultValue: false, submitOnChange: true
		}
		display2()
	}
}

void appButtonHandler(btn){
	def customCommand = btn.split("--")[-1]
	btn = btn.split("--")[0]
	
	//log.debug customCommand
	//log.debug btn
	
	switch(btn){
		case "RemoveCustomCommand":
			state.customCommands.remove(customCommand)
			break
	}
}

def updateButtonOptions(buttonsCustom,buttonsHSMIncluded,buttonsModesIncluded){
	if(logEnable) log.debug "includeButtonsCustom: ${includeButtonsCustom} - changeHSM: ${changeHSM} - changeModes: ${changeModes}"
	def options = []
	if(state.customCommands){
		options.addAll(state.customCommands)
	}
	if(includeButtonsCustom && buttonsCustom){
		options.addAll(buttonsCustom)
	}
	if(changeHSM && buttonsHSMIncluded){
		options.addAll(buttonsHSMIncluded)
	}
	if(changeModes && buttonsModesIncluded){
		options.addAll(buttonsModesIncluded)
	}
	return options
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
        } catch (e) {
			if(logEnable) log.debug "Virtual Keypad Child App unable to create keypad device - ${e}" 
			statusMessageD = "<b>Device Name (${dataName}) already exists, cannot create new keypad.  Refresh this page to try a new name.</b>"
			app.clearSetting("dataName")
		}
    } else {
        statusMessageD = "<b>Device Name (${dataName}) already exists.</b>"
    }
    return statusMessageD
}

def setDefaults() {
	if(logEnable == null){logEnable = true}
	/*
	if(changeHSM == null){changeHSM = true}
	if(changeModes == null){changeModes = true}
	if(includeButtonsCustom == null){includeButtonsCustom = true}
	*/
}

def getFormat(type, myText="") {			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}