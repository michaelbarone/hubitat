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
*
*  Change History:
*
*    Date        Who            What
*    ----        ---            ----
* 	 9-26-20	mbarone			initial release 
*/

def setVersion(){
    state.name = "Kodi Connector Child"
	state.version = "0.0.1"
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
	
	command "viewMotionCamera", [[name:"cameraID*",type:"NUMBER"]]
    command "viewAllCameras"
    command "sendClear"
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()   
}

def initialize() {
}

def sendClear(){
	log.debug "sendClear"

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
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
	)
    
    log.debug result
    result

}

def deviceNotification(message){
	log.debug "deviceNotification"
	
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
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
	)

    
   	runIn(1,sendClear)
    log.debug result    
    return result
}

def viewMotionCamera(cameraID = null){
	log.debug "viewMotionCamera"

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
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
	)

	runIn(1,sendClear)
	log.debug result
    return result
}

def viewAllCameras(){
	log.debug "viewAllCameras"

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
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
	)

	runIn(1,sendClear)
	log.debug result
    return result
}

def parse(String description) {
   // log.debug description
}