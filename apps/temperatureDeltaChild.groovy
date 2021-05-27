/**
 *  Temperature Delta Child
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
 * 	 21-5-25	mbarone			initial release 
 */
 
 def setVersion(){
    state.name = "Temperature Delta Child"
	state.version = "0.0.1"
}

definition(
    name: "Temperature Delta Child",
    namespace: "mbarone",
    author: "mbarone",
    description: "Calculate temperature delta between 2 selected sensor groups",
    category: "Convenience",
	parent: "mbarone:Temperature Delta Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/apps/temperatureDeltaChild.groovy",
)

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
	updated()
}

def updated() {
	state.showSupport = false
	unschedule()
    unsubscribe()
	initialize()
	updateSettings()
	if(logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
    if(logEnable) log.debug "Updated with settings: ${settings}"

    // average current group1 and group2 values and set delta device
    group1changed("auto")
    
    // set event listeners for group1 and 2 devices
    subscribe(group1, "temperature", group1changed)
    subscribe(group2, "temperature", group2changed)
}

def updateSettings(){
	app.updateLabel(label)
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("logEnable",[value:"false",type:"bool"])
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

/*
def notify(text){
	notifyDevices.each{
		it.deviceNotification(text)
	}
}
*/
preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
		display()

		if(!state.group1Temp){
			state.group1Temp = 0
		}
		if(!state.group2Temp){
			state.group2Temp = 0
		}

        section("<b>Instructions:</b>", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
    		paragraph "Create a virtual temperature device to view temperature deltas between 2 groups of devices."
    		paragraph "The Temperature Delta is calculated by taking Group2 temperature devices average and subtracting Group1 temperature devices average."
    		paragraph "If Group1 is 'inside house temperature sensors' and Group2 is 'outside house temp sensors' then the expected output would look like:"
    		paragraph "Group1 = 75; Group2 = 70; temperatureDeltaVirtualDevice = -5"
    		paragraph "Group1 = 78; Group2 = 85; temperatureDeltaVirtualDevice = 7"
		}
		
		section(getFormat("header-green", "Temperature Delta Device Creation")) {
			if(!dataName) {
                // add option to select existing temperature virtual device

				input "dataName", "text", title: "Enter a name for this Temperature Delta Vitual Device (ie. 'TempDelta-InsideToOutside' or 'Upstiars vs downstairs temp')", required:true, submitOnChange:true
				paragraph "<b>A device will automaticaly be created for you as soon as you click outside of this field.</b>"
			}
			if(dataName) {
				def statusMessageD = createTemperatureDeltaChildDevice()
			}
			if(statusMessageD == null) statusMessageD = "Waiting on status message..."
			paragraph "${statusMessageD}"
		}
        
		if(getChildDevice(dataName)) {

    		section(getFormat("header-green", "Group 1")) {
                input "group1", "capability.temperatureMeasurement", required: true, multiple: true,
						title: "Choose your group1 temperatureMeasurement devices"
			}

    		section(getFormat("header-green", "Group 2")) {
                input "group2", "capability.temperatureMeasurement", required: true, multiple: true,
						title: "Choose your group2 temperatureMeasurement devices"
			}

		}
		
        section(getFormat("header-green", "Maintenance")) {
			input "label", "string", title: "Enter a name for this Temperature Delta child app", required:false, submitOnChange:true, defaultValue:dataName
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
		case "ShowSettings":
			updateSettings()
            state.showSupport = true
			break

        /*	
		case "RefreshDisplay2":
			state.showSupport = false
			display2()
			break
        */
	}
}

def display() {
    setVersion()
    theName = app.label
    if(theName == null || theName == "") theName = "New Child Temperature Delta App"
    section (getFormat("title", "${state.name} - ${theName}")) {
		paragraph getFormat("line")
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>${state.name} - V${state.version}</div>"

		if(getChildDevice(dataName)) {
			paragraph ""
			paragraph ""
            /*
			getChildDevice(dataName).setVersion()
			pauseExecution(250)			
			state.VKDversion = getChildDevice(dataName).getState()?.version
			
			if(state.VKDversion==null){
				input "RefreshDisplay2--0", "button", title: "Refresh Version info"
				return
			}
			
			paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>Temperature Delta Device - V${state.VKDversion}</div>"
            */
		
    		input "ShowSettings--0", "button", title: "Get Settings For Support"
		
			if(state.showSupport){
				paragraph "[${state.name}: V${state.version}]"
				paragraph "${settings}"
			}
		}
	}       
}

def setDelta(){
    def tempDelta = state.group2Temp - state.group1Temp
    tempDelta = tempDelta.round(1)
    if(logEnable) log.debug("group2 $state.group2Temp - group1 $state.group1Temp = $tempDelta")
    getChildDevice(dataName).setTemperature(tempDelta)
}

def group1changed(evt){
    if(evt!="auto") {
	    def ave1 = evt.value
        def aveDev1 = evt.displayName
	    if(logEnable) log.debug("group1changed Received from: $aveDev1 - $ave1")
    } else {
         if(logEnable) log.debug("group1changed Received from: Auto Process")
    }

    def sumTemp = 0
    def countTemp = 0
    def meanTemp = 0
	def mean1Temp = 0
    def mean2Temp = 0
    
    for (sensor in settings.group1) {
        countTemp += 1 
	    if(logEnable) log.debug ( "Sensor data count = $countTemp" )
        sumTemp += sensor.currentTemperature 
    }
	if(logEnable) log.debug ( "Total Combined value =  $sumTemp")
	mean1Temp = sumTemp/countTemp
	mean2Temp = mean1Temp.toFloat()
    state.group1Temp = mean2Temp.round(1)
	if(logEnable) log.debug("Average group1Temp Temperature = $state.group1Temp")
    if(evt!="auto") {
        setDelta()
    } else {
        group2changed("auto")
    }
}

def group2changed(evt){
    if(evt!="auto") {
        def ave1 = evt.value
        def aveDev1 = evt.displayName
        if(logEnable) log.debug("group2changed Received from: $aveDev1 - $ave1")
    } else {
         if(logEnable) log.debug("group2changed Received from: Auto Process")
    }

    def sumTemp = 0
    def countTemp = 0
    def meanTemp = 0
	def mean1Temp = 0
    def mean2Temp = 0
    
    for (sensor in settings.group2) {
        countTemp += 1 
	    if(logEnable) log.debug ( "Sensor data count = $countTemp" )
        sumTemp += sensor.currentTemperature 
    }
	if(logEnable) log.debug ( "Total Combined value =  $sumTemp")
	mean1Temp = sumTemp/countTemp
	mean2Temp = mean1Temp.toFloat()
    state.group2Temp = mean2Temp.round(1)
	if(logEnable) log.debug("Average group2Temp Temperature = $state.group2Temp")
    setDelta()
}

def createTemperatureDeltaChildDevice() {
    if(logEnable) log.debug "Running createTemperatureDeltaChildDevice"
    statusMessageD = ""
    if(!getChildDevice(dataName)) {
        if(logEnable) log.debug "In createTemperatureDeltaChildDevice - Child device not found - Creating device: ${dataName}"
        try {
            addChildDevice("hubitat", "Virtual Temperature Sensor", dataName, 1234, ["name": "${dataName}", isComponent: false])
            if(logEnable) log.debug "In createTemperatureDeltaChildDevice - Child device has been created! (${dataName})"
            statusMessageD = "<b>Device has been been created. (${dataName})</b>"
        } catch (e) {
			if(logEnable) log.debug "Temperature Delta Child App unable to create TemperatureDelta device - ${e}" 
			statusMessageD = "<b>Device Name (${dataName}) already exists, cannot create new TemperatureDelta device.  Refresh this page to try a new name.</b>"
			app.clearSetting("dataName")
		}
    } else {
        statusMessageD = "<b>Device Name (${dataName}) already exists.</b>"
		if(logEnable) log.debug "TemperatureDelta Device (${dataName}) already exists."
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