/**
*  Camera Motion Capture Child
*
*
*  2020 mbarone
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
*  To use the video camera motion service, you must install and configure this on a local webserver
*  https://github.com/michaelbarone/CameraMotionCapture
*
*  Change History:
*
*    Date        Who            What
*    ----        ---            ----
* 	 20-11-4	mbarone			initial release 
* 	 20-11-25	mbarone			added additional logging
*/

def setVersion(){
    state.name = "Camera Motion Capture Child"
	state.version = "0.0.2"
}

metadata {
  	definition (name: "Camera Motion Capture Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/cameraMotionCaptureChild.groovy") {
		capability "Actuator"
  	}
	preferences {
        input name: "cameraURL", type: "text", title: "Camera Image URL", description: "The direct URL to get a snapshot from this camera", required: true
        input name: "captureCount", type: "number", title: "Capture Frame Count", description: "How many images do you want to save for each motion event (if empty, the value from the parent device will be used)"
        input name: "captureDelay", type: "number", title: "Delay Between Frames", description: "How many seconds between image captures for each motion event (if empty, the value from the parent device will be used)"
		input name: "username", type: "text", title: "Username", description: "Username if this camera requires authentication (if empty, the value from the parent device will be used)"
		input name: "password", type: "text", title: "Password", description: "Password if this camera requires authentication (if empty, the value from the parent device will be used)"
		input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)
	}
	
	command "captureEvent", [[name:"captureCount",type:"NUMBER"],[name:"captureDelay",type:"NUMBER"]]
	command "clearOldEvents", [[name:"daysToKeep*",type:"NUMBER",description:"Setting this to 0 will completely remove this camera from the server"]]
}

def installed() {
}

def updated() {
	def str = parent.getWebServerURL()
    if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == '/') {
        str = str.substring(0, str.length() - 1);
    }
	def cameraName = getDataValue("cameraName")
	if(cameraName != null){
		state.lastMotionEventImage = str+"/images/"+getDataValue("cameraName")+"/mostRecent.jpg"
 	}
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def initialize() {
}

def captureEvent(cCount=captureCount,cDelay=captureDelay){
	def cameraName = getDataValue("cameraName")
	if (logEnable) log.debug "capture event on: ${cameraName}"
	parent.captureEvent(cameraName,cameraURL,cCount,cDelay,username,password)
}

def clearOldEvents(daysToKeep){
	def cameraName = getDataValue("cameraName")
	parent.clearOldEvents(daysToKeep,cameraName)
}