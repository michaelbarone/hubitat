/**
 *  Virtual Basic Keypad
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
 * 	 10-06-20	mbarone			initial release 
 * 	 10-06-20	mbarone			added attribute Notification, which updates with a bad code input message if enabled.  this can be used to trigger a RM notification  if you watch this attribute for *changed* 
 * 	 03-10-21	mbarone			removed the SecurityKeypad capability as this was causing issues when present with the LockCodes capability and is not needed for the functionality of this addon
 * 	 11-04-21	mbarone			fixed security issue found by @arnb, and added an option to enable or disable.. enabled by default
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def setVersion(){
    state.name = "Virtual Basic Keypad"
	state.version = "1.0.4"
}

metadata {
	definition (name: "Virtual Basic Keypad", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/virtualBasicKeypad.groovy") {
		//commented out as this is not required for this addon to function.  It may be wanted by some users, but this causes issues when both the SecurityKeypad and LockCodes capability are enabled which breaks the ability to set lock codes on this device.
		//enable only if you need for your setup
		//capability "SecurityKeypad"

		capability "LockCodes"
		capability "PushableButton"
		capability "Momentary"
		capability "Actuator"
		capability "TamperAlert"
		capability "Contact Sensor"
	}

    preferences {
		input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: ""
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "cancelAlertsOnDisarm", type: "bool", title: "Cancel Alerts on Disarm", defaultValue: true
		input name: "src", type: "text", title: "iFrame Url", required: false, description: "paste the direct dashboard url for this keypad's dashboard"
		input name: "noCodeRequired", type: "text", title: "No Code Required", required: false, description: "These HSM commands require no code.  Separate multiple by comma:  armAway,armHome"
		input name: "noCodeRequiredDisarmedOnly", type: "bool", title: "HSM Disarmed required for No Code commands", required: true,defaultValue: true, description: "When 'On', HSM will need to be 'disarmed' to execute commands that do not require a code."
		input name: "armDelaySeconds", type: "number", title: "Command Delay", required: true, defaultValue: 0, description: "number of seconds before sending command after successful code entry"
		input name: "armDelaySecondsGroup", type: "text", title: "Commands to Delay", required: false, description: "These HSM commands will be delayed by the set Command Delay.  Separate multiple by comma:  armAway,armHome"
		input name: "alertOnFailedAttempts", type: "number", title: "Failed Attempts before Notify", required: true, defaultValue: 0, description: "Number of Failed Code entries before the 'Notification' attribute is updated with the failed notice.  0 = disabled"
	}

	attribute "Details","string"
	attribute "lastCodeName","string"
	attribute "InputDisplay","string"
	attribute "Keypad", "text"
	attribute "Notification", "text"
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
	clearCode()
	resetInputDisplay()
    updated()
    sendEvent(name:"maxCodes",value:20)
    sendEvent(name:"codeLength",value:4)
	state.panicPressCount = 0
	state.code = ""
}

def updated() {
	clearCode()
	close()
	clearDetails()
	state.armDelaySeconds = armDelaySeconds
	state.alertOnFailedAttemptsCount = 0
	sendEvent(name: "Notification", value: "")
	if(src){
		sendEvent(name: "Keypad", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
    }
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
}

def updateInputDisplay(text){
	sendEvent(name: "InputDisplay", value: text, displayed: false)
}

def commandHSM(action){
	if(checkInputCode(action)){
		if (logEnable) log.debug "commandHSM - ${action}"
		// cancel HSM alerts if set and mode has disarm in name
		if(cancelAlertsOnDisarm == true && action.toLowerCase().contains("disarm")){
			cancelHSMAlerts()
		}
		if(state.armDelaySeconds > 0 && armDelaySecondsGroup.contains(action)){
			def timeLeft = state.armDelaySeconds
			state.armDelaySeconds.times{
				timeLeft = timeLeft - 1
				updateInputDisplay("Setting ${action} in ${timeLeft} seconds")
				pauseExecution(1000)
			}
		}
		sendLocationEvent(name: "hsmSetArm", value: action, descriptionText: "Keypad Event ${action}")
		updateInputDisplay("Success.  Executing ${action}")
	} else {
		updateInputDisplay("Input Denied")
	}
	timeoutClearCode()
}

def timeoutClearCode(){
	unschedule(clearCode)
	clearCode()
	unschedule(resetInputDisplay)
	runIn(5,resetInputDisplay)	
}

def checkInputCode(action){
	if (logEnable) log.debug "checkInputCode - ${action}"
	
	def codeAccepted = false
	
	if(noCodeRequired.contains(action) && (!noCodeRequiredDisarmedOnly || noCodeRequiredDisarmedOnly && location.hsmStatus == 'disarmed')) {
		codeAccepted = true
		sendEvent(name:"UserInput", value: "Success", descriptionText: "No code was required to execute " + action, displayed: true)
		if (logEnable) log.debug "${action} executed with no entered code"
		return codeAccepted
	}

	if(state.code == ""){
		if (logEnable) log.debug "code is blank, returning false"
		return codeAccepted
	}

	Object lockCode = lockCodes.find{ it.value.code.toInteger() == state.code.toInteger() }
    if (lockCode){
        Map data = ["${lockCode.key}":lockCode.value]
        String descriptionText = "${device.displayName} executed ${action} by ${lockCode.value.name}"
        if (txtEnable) log.info "${descriptionText}"
        //if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        //sendEvent(name:"lock",value:"unlocked",descriptionText: descriptionText, type:"physical",data:data, isStateChange: true)
        sendEvent(name:"lastCodeName", value: lockCode.value.name, descriptionText: descriptionText, isStateChange: true, displayed: true)
		sendEvent(name:"UserInput", value: "Success", descriptionText: descriptionText + " " + action, displayed: true)
		codeAccepted = true
		if (logEnable) log.debug "${action} code accepted"
		state.alertOnFailedAttemptsCount = 0
    } else {
		state.alertOnFailedAttemptsCount = state.alertOnFailedAttemptsCount + 1
		unschedule(clearAlertOnFailedAttemptsCount)
		runIn(600,clearAlertOnFailedAttemptsCount)
		if(alertOnFailedAttempts > 0 && state.alertOnFailedAttemptsCount >= alertOnFailedAttempts){
			unschedule(clearAlertOnFailedAttemptsCount)
			state.alertOnFailedAttemptsCount = 0
			sendEvent(name: "Notification", value: "There have been ${alertOnFailedAttempts} failed code attempts.")
			unschedule(clearNotification)
			runIn(10,clearNotification)
		}
		sendEvent(name:"UserInput", value: "Failed", descriptionText: "Code input Failed for ${action} ("+state.code+")", displayed: true)
		if (logEnable) log.debug "${action} code NOT accepted"	
    }
	return codeAccepted
}

def clearDetails(){
	sendEvent(name:"Details", value:"Running Normally.")
}

def clearNotification(){
	sendEvent(name:"Notification", value:"")
}

def clearAlertOnFailedAttemptsCount(){
	state.alertOnFailedAttemptsCount = 0
}

def clearCode(){
	if (logEnable) log.debug "clearCode"
	state.panicPressCount = 0
	state.code = ""
	state.codeInput = "Enter Code"
	//state.codeInput = "HSM: ${location.hsmStatus} | Mode: ${location.mode} &#13; Enter Code"
}

def resetInputDisplay(){
	if (logEnable) log.debug "resetInputDisplay"
	updateInputDisplay(state.codeInput)
}

def panicAlarm(){
	state.panicPressCount = state.panicPressCount + 1
	if (logEnable) log.debug "panicAlarm press "+ state.panicPressCount
	if(state.panicPressCount<2){
		updateInputDisplay("Press Panic Again to Trigger")
	} else {
		// use contact and tamper:
		tamperAlert()
		open()
		updateInputDisplay("Panic Alarm Triggered")
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

def push(evt) {
	if(evt==null){
		return
	}
	unschedule(clearPanicCount)
	unschedule(clearCode)
	unschedule(resetInputDisplay)

	if(evt=="Clear"){
		clearCode()
		resetInputDisplay()
		return
	}

	if(evt=="Panic"){
		panicAlarm()
		return
	}

	if(evt.isNumber()){
		state.code = state.code+""+evt
		if(!state.codeInput.contains("*")){
			state.codeInput = "*"
		} else {
			state.codeInput = state.codeInput+"*"
		}
		updateInputDisplay(state.codeInput)
		unschedule(clearCode)
		runIn(30,clearCode)
		unschedule(resetInputDisplay)
		runIn(30,resetInputDisplay)
		return
	}
	
	commandHSM(evt)
	return
}

def open() {
	if (logEnable) log.debug "open() called"
	sendEvent(name: "contact", value: "open")
	runIn(5,close)
}

def close() {
	if (logEnable) log.debug "close() called"
	sendEvent(name: "contact", value: "closed")
}

def tamperAlert(){
	if (logEnable) log.debug "tamperAlert() called"
	sendEvent(name: "tamper", value: "detected", isStateChange  : true)
	runIn(5,clearTamperAlert)
}

def clearTamperAlert(){
	if (logEnable) log.debug "clearTamperAlert() called"
	sendEvent(name: "tamper", value: "clear", isStateChange  : true)
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