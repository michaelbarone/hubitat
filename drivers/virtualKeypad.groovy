/**
 *  Virtual Keypad
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
 * 	 10-01-20	mbarone			can start inputting new code immediately after bad code response without waiting for the 5 second reset 
 * 	 10-01-20	mbarone			added Panic button integration
 * 	 10-03-20	mbarone			added SecurityKeypad capability to help integrate into HSM and other apps that use this feature
 * 	 10-05-20	mbarone			added lastCodeName attribute so RM could pick up the changes
 * 	 10-08-20	mbarone			only adds child devices that are selected in the app, will remove child devices that are not selected in the app
 * 	 10-16-20	mbarone			code/comment cleanup
 * 	 11-18-20	mbarone			added delay to custom re-arm command, if re-arm was not added to the command delay timer it sometimes wouldnt process the arm command as disarm was still executing
 * 	 11-18-20	mbarone			added selected commands can cancel HSM alerts when triggered
 * 	 02-10-21	mbarone			added option to cancel count down timer and include optional chime child device to trigger chimes/etc using RM when countdown is active
 * 	 02-13-21	mbarone			bugfix - timer errors out when delay chime is not configured.
 * 	 02-14-21	mbarone			added preference to customize inputDisplay default text plus bugfix - input display did not give feedback for bad code input.
 * 	 02-15-21	mbarone			change code timeout to clear faster after bad code input.
 * 	 02-15-21	mbarone			bugfix - forced setting a standard InputDisplayDefaultText if value is null which was causing issues with users upgrading as it wasnt getting set by default for some reason.
 * 	 02-15-21	mbarone			bugfix - properly handle cancel timer function.  cleaned up some associated functions
 * 	 02-16-21	mbarone			added support options to get a summary of versions and keypad settings to help troubleshoot issues
 * 	 03-10-21	mbarone			removed the SecurityKeypad capability as this was causing issues when present with the LockCodes capability and is not needed for the functionality of this addon
 * 	 07-02-21	mbarone			Added new "button" attribute, which adds a button to a dashboard which will show the keypad iframe after clicking, this saves dashboard real estate when not using the keypad
 * 	 07-12-21	mbarone			updated autoClose logic timeout for closing the overlay iframe if that option is selected
 * 	 07-29-21	mbarone			added option to Attempt Auto Centering of keypad when overley iframe is used
 * 	 11-04-21	mbarone			fixed security issue found by @arnb and @scubamikejax904, and added an option to enable or disable.. enabled by default
 * 	 12-05-21	mbarone			surfaced other modal options in preferences
 */

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def setVersion(){
	state.version = "1.0.22"
}
 
metadata {
	definition (name: "Virtual Keypad", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualKeypad.groovy") {
		//commented out as this is not required for this addon to function.  It may be wanted by some users, but this causes issues when both the SecurityKeypad and LockCodes capability are enabled which breaks the ability to set lock codes on this device.
		//enable only if you need for your setup
		//capability "SecurityKeypad"

		capability "LockCodes"
	}

    preferences {
		input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: ""
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
		input name: "src", type: "text", title: "Keypad Dashboard Url for iFrame",  required: false
		input name: "InputDisplayDefaultText", type: "text", title: "Default Text to display in Keypad Input Display",  required: true, defaultValue: "Enter Code"

		input name: "text", type: "text", title: "Set remaining preferences for Dashboard Button to hide/show keypad on dashbaords"

        input("openText", "text", title: "Button text to Open Keypad iFrame", defaultValue:"Keypad",  required: false)
        input("closeText", "text", title: "Button text to close Keypad iFrame", defaultValue:"Close", required: false)
        input("refreshText", "text", title: "Button text to refresh Keypad iFrame", defaultValue:"Refresh", required: false)
		input("keypadWidth", "number", title: "Width of keypad when in view as a percentage (default: 50)", defaultValue:50, required: false)
		input("keypadHeight", "number", title: "Height of keypad when in view as a percentage (default: 100)", defaultValue:100, required: false)
		input("keypadAutoCenter", "bool", title: "Try to Auto Center the keypad when in view", defaultValue:false, required: false)
		input("AutoCloseDelay", "number", title: "Number of seconds to automatically close the Keypad after opening it with dashboard button (default: 60)", defaultValue:60, required: false)
        input("modalCSS", "text", title: "Style to apply to modal", description: "defaults: position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85);", defaultValue:"position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85);", required: false)
	}

	attribute "Details","string"
	attribute "lastCodeName","string"
	attribute "Keypad", "text"
	attribute "Button", "text"
}

def configureSettings(settings){
	state.noCodeRequired = ["none"]
	state.noCodeRequiredDisarmedOnly = true
	state.defaultMode = ""
	state.defaultModeTrigger = ["none"]
	state.armDelaySecondsGroup = ["none"]
	state.panicPressCount = 0
	state.notifyCount = 0	
	settings.each{
		switch(it.key){
			case "armDelay":
				state.armDelay = it.value
				break

			case "armDelaySeconds":
				state.armDelaySeconds = it.value
				break
	
			case "armDelaySecondsGroup":
				state.armDelaySecondsGroup = it.value
				break
				
			case "changeHSM":
				state.changeHSM = it.value
				break
				
			case "changeModes":
				state.changeModes = it.value
				break
				
			case "notify":
				state.notify = it.value
				break
				
			case "notifyLimit":
				state.notifyLimit = it.value
				break			
				
			case "noCodeRequired":
				state.noCodeRequired = it.value
				break			
				
			case "noCodeRequiredDisarmedOnly":
				state.noCodeRequiredDisarmedOnly = it.value
				break
				
			case "defaultMode":
				state.defaultMode = it.value
				break

			case "defaultModeTrigger":
				state.defaultModeTrigger = it.value
				break	

			case "cancelAlertsOnDisarm":
				state.cancelAlertsOnDisarm = it.value
				break
				
			case "cancelAlertsOnCommands":
				state.cancelAlertsOnCommands = it.value
				break
				
			case "availableButtons":
				state.availableButtons = it.value
				break	
				
			case "chimeDelay":
				state.chimeDelay = it.value
				break
				
			case "chimeTiming":
				state.chimeTiming = it.value
				break

			case "chimeDelayGroup":
				state.chimeDelayGroup = it.value
				break			
		}
	}
	updated()
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
	setVersion()
	clearCode()
	resetInputDisplay()
    clearDetails()
    sendEvent(name:"maxCodes",value:20)
    sendEvent(name:"codeLength",value:4)
	state.notifyCount = 0
	state.panicPressCount = 0
	state.countdownRunning = false
}

def updated() {
	setVersion()
	if(InputDisplayDefaultText == null) {
		log.info "setting InputDisplayDefaultText as it was previously unset"
		device.updateSetting("InputDisplayDefaultText",[value:"Enter Code",type:"text"])
	}
	createChildren()
	clearDetails()
	if(device.currentValue("Keypad") != src){
		if(src != ""){
			sendEvent(name: "Keypad", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
		} else {
			sendEvent(name: "Keypad", value: "")
		}
	}

	if(modalCSS == null) {
		log.info "setting modalCSS as it was previously unset"
		device.updateSetting("modalCSS",[value:"position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85)",type:"text"])
	}

	if(keypadWidth>90){
		sendEvent(name: "Button", value: "<button onclick=\"document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='block';autoClose = setTimeout(function(){document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'},${AutoCloseDelay}000);\">${openText}</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;z-index:100;${modalCSS}'><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none';clearTimeout(autoClose); style='float:right;margin:5px;'>${closeText}</button><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:right;margin:5px;'>${refreshText}</button><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=${src} style='height:95%;width:${keypadWidth}%;border:none;'></iframe></div>")
	} else {
		if(keypadAutoCenter){
			sendEvent(name: "Button", value: "<button onclick=\"document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='block';autoClose = setTimeout(function(){document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'},${AutoCloseDelay}000);\">${openText}</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;z-index:100;${modalCSS}'><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none';clearTimeout(autoClose); style='float:right;margin:5px;'>${closeText}</button><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:right;margin:5px;'>${refreshText}</button><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=${src} style='height:${keypadHeight}%;width:${keypadWidth}%;border:none;'></iframe></div>")
		} else {
			sendEvent(name: "Button", value: "<button onclick=\"document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='block';autoClose = setTimeout(function(){document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'},${AutoCloseDelay}000);\">${openText}</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;z-index:100;${modalCSS}'><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none';clearTimeout(autoClose); style='float:right;margin:5px;'>${closeText}</button><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:right;margin:5px;'>${refreshText}</button><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=${src} style='height:${keypadHeight}%;width:${keypadWidth}%;border:none;left:0;position:absolute;'></iframe></div>")
		}
	}

	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
	state.countdownRunning = false
	unschedule(clearCode)
	runIn(1,clearCode)
	unschedule(resetInputDisplay)
	runIn(1,resetInputDisplay)
}

def commandMode(action,btn){
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	// set default mode if set
	if(state.defaultMode != "" && state.defaultModeTrigger.any{btn.contains(it)}){
		parent.setMode(state.defaultMode)
	}
	if(state.changeModes){
		parent.setMode(action)
	}
	displayDevice?.updateInputDisplay("Success.  Executing ${action}")
	def commandChildDevice = getChildDevice("${device.deviceNetworkId}-${btn}")
	commandChildDevice?.on()
	timeoutClearCode()
}

def commandHSM(action,btn){
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	if(state.changeHSM){
		sendLocationEvent(name: "hsmSetArm", value: action, descriptionText: "Keypad Event ${action}")
	}
	displayDevice?.updateInputDisplay("Success.  Executing ${action}")
	def commandChildDevice = getChildDevice("${device.deviceNetworkId}-${btn}")
	commandChildDevice?.on()
	timeoutClearCode()
}

def commandCustom(action,btn){
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	if(action=="ReArm"){
		displayDevice?.updateInputDisplay("Success.  Executing Arm")
		getChildDevice("${device.deviceNetworkId}-Custom-Arm")?.on()
	} else {		
		displayDevice?.updateInputDisplay("Success.  Executing ${action}")
		getChildDevice("${device.deviceNetworkId}-${btn}")?.on()
	}
	timeoutClearCode()
}

def timeoutClearCode(){
	unschedule(clearCode)
	clearCode()
	unschedule(resetInputDisplay)
	runIn(5,resetInputDisplay)	
}

def checkInputCode(btn){
	if (logEnable) log.debug "checkInputCode"
	
	def codeAccepted = false
	
	if(state.noCodeRequired.any{btn.contains(it)} && (!state.noCodeRequiredDisarmedOnly || (state.noCodeRequiredDisarmedOnly && location.hsmStatus == 'disarmed'))) {
		codeAccepted = true
		sendEvent(name:"UserInput", value: "Success", descriptionText: "No code was required to execute " + btn, displayed: true)
		if (logEnable) log.debug "${btn} executed with no entered code"
		return codeAccepted
	}

	if(state.code == ""){
		if (logEnable) log.debug "code is blank, returning false"
		return codeAccepted
	}

	Object lockCode = lockCodes.find{ it.value.code.toInteger() == state.code.toInteger() }
    if (lockCode){
		state.notifyCount = 0			
		if(state.countdownRunning){
			String descriptionText = "${device.displayName} executed Cancel Count Down Timer by ${lockCode.value.name}"
			if (txtEnable) log.info "${descriptionText}"
			sendEvent(name:"lastCodeName", value: lockCode.value.name, descriptionText: descriptionText, isStateChange: true, displayed: true)
			sendEvent(name:"UserInput", value: "Success", descriptionText: descriptionText, displayed: true)
			if (logEnable) log.debug "countdown cancelled code accepted"		
			unschedule(countDownTimer)
			def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
			displayDevice?.updateInputDisplay("Countdown Cancelled.")
			timeoutClearCode()
			return true
		} else {
			//Map data = ["${lockCode.key}":lockCode.value]
			//if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
			//sendEvent(name:"lock",value:"unlocked",descriptionText: descriptionText, type:"physical",data:data, isStateChange: true)
			String descriptionText = "${device.displayName} executed ${btn} by ${lockCode.value.name}"
			if (txtEnable) log.info "${descriptionText}"
			sendEvent(name:"lastCodeName", value: lockCode.value.name, descriptionText: descriptionText, isStateChange: true, displayed: true)
			sendEvent(name:"UserInput", value: "Success", descriptionText: descriptionText + " " + btn, displayed: true)
			codeAccepted = true
			if (logEnable) log.debug "${btn} code accepted"
		}
    } else {
		sendEvent(name:"UserInput", value: "Failed", descriptionText: "Code input Failed for ${btn} ("+state.code+")", displayed: true)
		if (logEnable) log.debug "${btn} code NOT accepted"	
		state.notifyCount = state.notifyCount + 1
		unschedule(clearNotifyCount)
		runIn(60,clearNotifyCount)
		if(state.notify && state.notifyCount.toInteger() >= state.notifyLimit.toInteger()){
			parent.notify("Keypad code input Failed triggered by ${btn} ("+state.code+")")
			state.notifyCount = 0
			unschedule(clearNotifyCount)
		}
    }
	return codeAccepted
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

def clearNotifyCount(){
	state.notifyCount = 0
}

def clearCode(){
	if (logEnable) log.debug "clearCode"
	state.panicPressCount = 0
	state.code = ""
	if(InputDisplayDefaultText == null){
		state.codeInput = "Enter Code"
	} else {
		state.codeInput = InputDisplayDefaultText
	}
	//state.codeInput = "HSM: ${location.hsmStatus} | Mode: ${location.mode} &#13; Enter Code"
}

def resetInputDisplay(){
	if (logEnable) log.debug "resetInputDisplay"
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	displayDevice?.updateInputDisplay(state.codeInput)
}

def panicAlarm(){
	state.panicPressCount = state.panicPressCount + 1
	if (logEnable) log.debug "panicAlarm press "+ state.panicPressCount
	if(state.panicPressCount<2){
		getChildDevice("${device.deviceNetworkId}-InputDisplay")?.updateInputDisplay("Press Panic Again to Trigger")
	} else {
		def panicDevice = getChildDevice("${device.deviceNetworkId}-Panic")
		panicDevice?.tamperAlert()
		panicDevice?.on()
		getChildDevice("${device.deviceNetworkId}-InputDisplay")?.updateInputDisplay("Panic Alarm Triggered")
		state.panicPressCount = 0
	}
	unschedule(clearPanicCount)
	runIn(5,clearPanicCount)
}

def clearPanicCount(){
	if (logEnable) log.debug "clearPanicCount"
	unschedule(clearCode)
	clearCode()
	unschedule(resetInputDisplay)
	resetInputDisplay()
}

def cancelHSMAlerts(){
	if (logEnable) log.debug "cancelHSMAlerts"
	sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts", descriptionText: "Keypad Event HSM cancelAlerts")
}

def buttonPress(btn) {
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	unschedule(clearPanicCount)
	unschedule(clearCode)
	unschedule(resetInputDisplay)

	if(btn=="Clear"){
		clearCode()
		resetInputDisplay()
		return
	}

	if(btn=="Panic"){
		panicAlarm()
		return
	}

	if(btn.isNumber()){
		state.code = state.code+""+btn
		if(!state.codeInput.contains("*")){
			state.codeInput = "*"
		} else {
			state.codeInput = state.codeInput+"*"
		}
		if(!state.countdownRunning){
			displayDevice?.updateInputDisplay(state.codeInput)
		}
		unschedule(clearCode)
		runIn(30,clearCode)
		unschedule(resetInputDisplay)
		runIn(30,resetInputDisplay)
		return
	}

	if(btn == null){
		return
	}

	if(checkInputCode(btn)){
		clearCode()
		if(state.countdownRunning == true){		
			state.countdownRunning = false
			return
		}
	} else {
		displayDevice?.updateInputDisplay("Input Denied")
		unschedule(clearCode)
		clearCode()
		unschedule(resetInputDisplay)
		runIn(5,resetInputDisplay)			
		return
	}
	
	def type = btn.split("-")[-2]
	def action = btn.split("-")[-1]
	
	unschedule(countDownTimer)
	state.countdownRunning = true
	countDownTimer(type,action,btn)
	return
}

def countDownTimer(type,action,btn,secondsLeft=-1){
	def displayDevice = getChildDevice("${device.deviceNetworkId}-InputDisplay")
	if(secondsLeft==-1){
		if(action=="ReArm"){
			displayDevice?.updateInputDisplay("Success.  Executing Disarm")
			getChildDevice("${device.deviceNetworkId}-Custom-Disarm")?.on()
			pauseExecution(2000)
		}
		if(!state.armDelay || !state.armDelaySecondsGroup.any{btn.contains(it)}){
		} else {
			secondsLeft = state.armDelaySeconds
		}
	}
	if(secondsLeft>0){	
		unschedule(countDownTimer)
		if(state.countdownRunning == true){
			if(state.codeInput.contains("*")){
				displayDevice?.updateInputDisplay("Setting ${action} in ${secondsLeft} seconds | ${state.codeInput}")
			} else {
				displayDevice?.updateInputDisplay("Setting ${action} in ${secondsLeft} seconds")
			}
			if(state.chimeDelay && state.chimeDelayGroup.any{btn.contains(it)}){
				def timeCheck = secondsLeft/state.chimeTiming
				if("${timeCheck}".isInteger()){
					getChildDevice("${device.deviceNetworkId}-Chime")?.on()
				}
			}
			secondsLeft = secondsLeft - 1
			runIn(1,countDownTimer,[data: [type,action,btn,secondsLeft]])
		}
		return
	}
	
	state.countdownRunning = false

	if(state.cancelAlertsOnDisarm == true && (btn.toLowerCase().contains("disarm") || state.cancelAlertsOnCommands.any{btn.contains(it)})){
		cancelHSMAlerts()
	}

	if(type=="Mode"){
		if (logEnable) log.debug "button is mode"
		commandMode(action,btn)
		return
	}

	if(type=="HSM"){
		if (logEnable) log.debug "button is HSM"
		commandHSM(action,btn)
		return
	}
	
	if(type=="Custom"){
		if (logEnable) log.debug "button is Custom"
		commandCustom(action,btn)
		return
	}
}


def createChildren(){
    if (logEnable) log.debug "Creating Child Devices"

	// create all buttons
	def theCommands = state.availableButtons.clone()
	if(state.chimeDelay){
		theCommands.addAll(["Clear","Number","Panic","Chime"])
	} else {
		theCommands.addAll(["Clear","Number","Panic"])
	}
	//log.debug theCommands
	def allButtons = ["InputDisplay"]
	allButtons.addAll(theCommands)
	

	
	// create all buttons
	theCommands.each {
		foundChildDevice = null
		foundChildDevice = getChildDevice("${device.deviceNetworkId}-${it}")
	
		if(foundChildDevice=="" || foundChildDevice==null){
	
			if (logEnable) log.debug "createChildDevice:  Creating Child Device '${device.displayName} (${it})'"
			try {
				def deviceHandlerName = "Virtual Keypad Button Child"
				addChildDevice(deviceHandlerName,
								"${device.deviceNetworkId}-${it}",
								[
									completedSetup: true, 
									label: "${device.displayName} (${it})", 
									isComponent: true, 
									name: "${device.displayName} (${it})",
									componentName: "${it}", 
									componentLabel: "${it}"
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
	
	if (logEnable) log.debug "removing any child devices that are no longer selected in app"
	childDevices.each {
		try{
			if (logEnable) log.debug "checking ${it.deviceNetworkId}"
			//def command = it.deviceNetworkId.split("-")[-1]
			def command = it.deviceNetworkId.replace("${device.deviceNetworkId}-","")
			if(allButtons.contains(command)){
			} else {
				if (logEnable) log.debug "removing ${it.deviceNetworkId}" 
				deleteChildDevice(it.deviceNetworkId)
			}
		}
		catch (e) {
			if (logEnable) log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}	
	
	// create input display
	['InputDisplay'].each {
		foundChildDevice = null
		foundChildDevice = getChildDevice("${device.deviceNetworkId}-${it}")
	
		if(foundChildDevice=="" || foundChildDevice==null){
	
			if (logEnable) log.debug "createChildDevice:  Creating Child Device '${device.displayName} (${it})'"
			try {
				def deviceHandlerName = "Virtual Keypad Input Display Child"
				addChildDevice(deviceHandlerName,
								"${device.deviceNetworkId}-${it}",
								[
									completedSetup: true, 
									label: "${device.displayName} (${it})", 
									isComponent: true, 
									name: "${device.displayName} (${it})",
									componentName: "${it}", 
									componentLabel: "${it}"
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
}

def removeChildren(){
	if (logEnable) log.debug "removing any child devices"
	childDevices.each {
		try{
			if (logEnable) log.debug "removing ${it.deviceNetworkId}"
			deleteChildDevice(it.deviceNetworkId)
		}
		catch (e) {
			if (logEnable) log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
	sendEvent(name:"Details", value:"Child devices removed!  Run the createChildren function when ready to re-build child devices.")
	unschedule(clearDetails)
	runIn(300,clearDetails)
}
 
def removeChild(childNID){
	try{
		if (logEnable) log.debug "removing ${childNID}"
		deleteChildDevice(childNID)
		sendEvent(name:"Details", value:"Child device (${childNID}) removed!")
		unschedule(clearDetails)
		runIn(300,clearDetails)
	}
	catch (e) {
		if (logEnable) log.debug "Error deleting ${childNID}: ${e}"
	}
}


void setCodeLength(length){
    /*
	on install/configure/change
		name		value
		codeLength	length
	*/
    String descriptionText = "${device.displayName} codeLength set to ${length}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"codeLength",value:length,descriptionText:descriptionText)
}

void setCode(codeNumber, code, name = null) {
    /*
	on sucess
		name		value								data												notes
		codeChanged	added | changed						[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]	default name to code #<codeNumber>
		lockCodes	JSON map of all lockCode
	*/
 	if (codeNumber == null || codeNumber == 0 || code == null) return

    if (logEnable) log.debug "setCode- ${codeNumber}"	
	
    if (!name) name = "code #${codeNumber}"

    Map lockCodes = getLockCodes()
    Map codeMap = getCodeMap(lockCodes,codeNumber)
    if (!changeIsValid(lockCodes,codeMap,codeNumber,code,name)) return
	
   	Map data = [:]
    String value
	
    if (logEnable) log.debug "setting code ${codeNumber} to ${code} for lock code name ${name}"

    if (codeMap) {
        if (codeMap.name != name || codeMap.code != code) {
            codeMap = ["name":"${name}", "code":"${code}"]
            lockCodes."${codeNumber}" = codeMap
            data = ["${codeNumber}":codeMap]
            if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
            value = "changed"
        }
    } else {
        codeMap = ["name":"${name}", "code":"${code}"]
        data = ["${codeNumber}":codeMap]
        lockCodes << data
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        value = "added"
    }
    updateLockCodes(lockCodes)
    sendEvent(name:"codeChanged",value:value,data:data, isStateChange: true)
}

void deleteCode(codeNumber) {
    /*
	on sucess
		name		value								data
		codeChanged	deleted								[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]
		lockCodes	[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"],<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]
	*/
    Map codeMap = getCodeMap(lockCodes,"${codeNumber}")
    if (codeMap) {
		Map result = [:]
        //build new lockCode map, exclude deleted code
        lockCodes.each{
            if (it.key != "${codeNumber}"){
                result << it
            }
        }
        updateLockCodes(result)
        Map data =  ["${codeNumber}":codeMap]
        //encrypt lockCode data is requested
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        sendEvent(name:"codeChanged",value:"deleted",data:data, isStateChange: true)
    }
}


//helpers
Boolean changeIsValid(lockCodes,codeMap,codeNumber,code,name){
    //validate proposed lockCode change
    Boolean result = true
    Integer maxCodeLength = device.currentValue("codeLength")?.toInteger() ?: 4
    Integer maxCodes = device.currentValue("maxCodes")?.toInteger() ?: 20
    Boolean isBadLength = code.size() > maxCodeLength
    Boolean isBadCodeNum = maxCodes < codeNumber
    if (lockCodes) {
        List nameSet = lockCodes.collect{ it.value.name }
        List codeSet = lockCodes.collect{ it.value.code }
        if (codeMap) {
            nameSet = nameSet.findAll{ it != codeMap.name }
            codeSet = codeSet.findAll{ it != codeMap.code }
        }
        Boolean nameInUse = name in nameSet
        Boolean codeInUse = code in codeSet
        if (nameInUse || codeInUse) {
            if (nameInUse) { log.warn "changeIsValid:false, name:${name} is in use:${ lockCodes.find{ it.value.name == "${name}" } }" }
            if (codeInUse) { log.warn "changeIsValid:false, code:${code} is in use:${ lockCodes.find{ it.value.code == "${code}" } }" }
            result = false
        }
    }
    if (isBadLength || isBadCodeNum) {
        if (isBadLength) { log.warn "changeIsValid:false, length of code ${code} does not match codeLength of ${maxCodeLength}" }
        if (isBadCodeNum) { log.warn "changeIsValid:false, codeNumber ${codeNumber} is larger than maxCodes of ${maxCodes}" }
        result = false
    }
    return result
}

Map getCodeMap(lockCodes,codeNumber){
    Map codeMap = [:]
    Map lockCode = lockCodes?."${codeNumber}"
    if (lockCode) {
        codeMap = ["name":"${lockCode.name}", "code":"${lockCode.code}"]
    }
    return codeMap
}

Map getLockCodes() {
    /*
	on a real lock we would fetch these from the response to a userCode report request
	*/
    String lockCodes = device.currentValue("lockCodes")
    Map result = [:]
    if (lockCodes) {
        //decrypt codes if they're encrypted
        if (lockCodes[0] == "{") result = new JsonSlurper().parseText(lockCodes)
        else result = new JsonSlurper().parseText(decrypt(lockCodes))
    }
    return result
}

void getCodes() {
    //no op
}

void updateLockCodes(lockCodes){
    /*
	whenever a code changes we update the lockCodes event
	*/
    if (logEnable) log.debug "updateLockCodes: ${lockCodes}"
    String strCodes = JsonOutput.toJson(lockCodes)
    if (optEncrypt) {
        strCodes = encrypt(strCodes)
    }
    sendEvent(name:"lockCodes", value:strCodes, isStateChange:true)
}

void updateEncryption(){
    /*
	resend lockCodes map when the encryption option is changed
	*/
    String lockCodes = device.currentValue("lockCodes") //encrypted or decrypted
    if (lockCodes){
        if (optEncrypt && lockCodes[0] == "{") {	//resend encrypted
            sendEvent(name:"lockCodes",value: encrypt(lockCodes))
        } else if (!optEncrypt && lockCodes[0] != "{") {	//resend decrypted
            sendEvent(name:"lockCodes",value: decrypt(lockCodes))
        }
    }
}