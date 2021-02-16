/**
*  Kodi Connector Child
*
*
*  2019 Stephan Hackett original code - updated and modified by mbarone
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
* This driver uses sections of code derived from the original Kodi Media Center driver developed by @josh (Josh Lyon)
*
*  To use the video camera view, you must install and configure this plugin for KODI:
*  https://github.com/michaelbarone/script.securitycam
*
*  Change History:
*
*    Date        Who            What
*    ----        ---            ----
* 	 20-9-26	mbarone			initial release 
* 	 21-1-04	mbarone			added non-blocking option for hubaction calls to help with sending to multiple child devices at once from parent device 
*/

def setVersion(){
    state.name = "Kodi Connector Child"
	state.version = "0.0.2"
}

metadata {
  	definition (name: "Kodi Connector Child", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/kodiConnectorChild.groovy") {
    	capability "Notification"
		capability "Actuator"
  	}
	preferences {
		input("ip", "text", title: "Kodi Client Ip Address", description: "", required:true)
		input("port", "text", title: "Kodi Client Port", description: "", required:true)
		input("title", "text", title: "Notification Title", description: "", required:true)
		input("username", "text", title: "Kodi Client Username", description: "")
		input("password", "text", title: "Kodi Client Password", description: "")
		//input("image", "text", title: "Image", description: "")
		input("displaytime", "number", title: "Notification Timeout(seconds)", description: "")
		input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)
	}
	
	command "viewMotionCameraID", [[name:"cameraID*",type:"NUMBER",description:"The Camera ID from the KODI plugin"]]
	command "viewMotionCameraDirect", [[name:"cameraName*",type:"STRING"],[name:"cameraURL*",type:"STRING",description:"The Camera image URL"],[name:"cameraUsername",type:"STRING"],[name:"cameraPassword",type:"STRING"]]
	command "viewAllCamerasDirect", [[name:"camera1*",type:"STRING",description:"The Camera image URL"],[name:"camera2",type:"STRING",description:"The Camera image URL"],[name:"camera3",type:"STRING",description:"The Camera image URL"],[name:"camera4",type:"STRING",description:"The Camera image URL"],[name:"cameraUsername",type:"STRING"],[name:"cameraPassword",type:"STRING"]]
    command "viewAllCameras"
    command "sendClear"

}

def installed() {
 	initialize()
}

def updated() {
 	initialize()
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

def sendToKODI(content){
    //BUILD HEADER    
    def myHeaders = [
        "HOST": ip + ":" + port,
        "Content-Type":"application/json"
    ]
    if(username){
    	def pair ="$username:$password"
        def basicAuth = pair.bytes.encodeBase64();
    	myHeaders.put("Authorization", "Basic " + basicAuth )
    }
    
    //SEND NOTIFICATION
    def result = new hubitat.device.HubAction(
		[
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
		],
        "string",
		[
		timeout: 1
		]
	)
    
    if (logEnable) log.debug result
    return result
}

def sendClear(){
	if (logEnable) log.debug "sendClear"

    //BUILD PARAMS
    def myParams = [
        "action":"noop"
    ]
    
    //BUILD BODY
    def content = [
       "jsonrpc":"2.0",
        "method":"Input.ExecuteAction",
        "params": myParams,
        "id":1
    ]

	sendToKODI(content)
}

def deviceNotification(message){
	if (logEnable) log.debug "deviceNotification"
	
    //BUILD PARAMS
    def myParams = [
        "title":title,
        "message":message
    ]
    //if(image) myParams.put("image", image)
    if(displaytime) myParams.put("displaytime", displaytime*1000)
    
    //BUILD BODY
    def content = [
       "jsonrpc":"2.0",
        "method":"GUI.ShowNotification",
        "params": myParams,
        "id":1
    ]

	runIn(1, sendToKODI, [data: content])
}

def viewAllCameras(){
	if (logEnable) log.debug "viewAllCameras"

    //BUILD PARAMS
	def myParams = [
		"addonid":"script.securitycam"
	]
	
    //BUILD BODY
    def content = [
		"jsonrpc":"2.0",
		"method":"Addons.ExecuteAddon",
		"params": myParams,
		"id":1
    ]

	runIn(2,sendClear)
	runIn(1, sendToKODI, [data: content])
}

def viewAllCamerasDirect(camera1,camera2="",camera3="",camera4="",cameraUsername="",cameraPassword=""){
	if (logEnable) log.debug "viewAllCamerasDirect"

    //BUILD PARAMS
	def myParams = [
		"addonid":"script.securitycam",
		"params":[
			"requestType":"display",
			"camera1":camera1,
			"camera2":camera2,
			"camera3":camera3,
			"camera4":camera4,
			"cameraUsername":cameraUsername,
			"cameraPassword":cameraPassword
		]
	]
	
    //BUILD BODY
    def content = [
		"jsonrpc":"2.0",
		"method":"Addons.ExecuteAddon",
		"params": myParams,
		"id":1
    ]

	runIn(2,sendClear)
	runIn(1, sendToKODI, [data: content])
}

def viewMotionCameraID(cameraID = null){
	if (logEnable) log.debug "viewMotionCameraID"

    //BUILD PARAMS
	def myParams = [
		"addonid":"script.securitycam",
		"params":[
			"streamid":"${cameraID}",
			"requestType":"motion"
		]
	]
	
    //BUILD BODY
    def content = [
		"jsonrpc":"2.0",
		"method":"Addons.ExecuteAddon",
		"params": myParams,
		"id":1
    ]
	
	runIn(2,sendClear)
	runIn(1, sendToKODI, [data: content])
}

def viewMotionCameraDirect(cameraName,cameraURL,cameraUsername="",cameraPassword=""){
	if (logEnable) log.debug "viewMotionCameraDirect"

    //BUILD PARAMS
	def myParams = [
		"addonid":"script.securitycam",
		"params":[
			"requestType":"motion",
			"cameraName":cameraName,
			"cameraURL":cameraURL,
			"cameraUsername":cameraUsername,
			"cameraPassword":cameraPassword
		]
	]
	
    //BUILD BODY
    def content = [
		"jsonrpc":"2.0",
		"method":"Addons.ExecuteAddon",
		"params": myParams,
		"id":1
    ]

	runIn(2,sendClear)
	runIn(1, sendToKODI, [data: content])
}

def parse(String description) {
   // if (logEnable) log.debug description
}