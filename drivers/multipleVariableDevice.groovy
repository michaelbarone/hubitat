/**
*  Multiple Variable Device
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
* 	 21-07-12	mbarone			initial release 
*/

def setVersion(){
    state.name = "Multiple Variable Device"
	state.version = "0.0.1"
}

metadata {
  	definition (name: "Multiple Variable Device", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/multipleVariableDevice.groovy") {
		capability "Actuator"
   	
        attribute "combined", "String"
        attribute "variable1", "String"
        attribute "variable2", "String"
        attribute "variable3", "String"
        attribute "variable4", "String"
	    command "setVariable1", [[name:"variable",type:"STRING"]]
	    command "setVariable2", [[name:"variable",type:"STRING"]]
	    command "setVariable3", [[name:"variable",type:"STRING"]]
	    command "setVariable4", [[name:"variable",type:"STRING"]]
    }
	preferences {
 		input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)
	}

}

def installed() {
}

def updated() {
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

def setVariable(varNum, data){
    sendEvent(name: varNum, value: data)
    unschedule(setCombined)
    runIn(5,setCombined)
}

def setCombined(){
    def data = ""
    if(device.currentValue("variable1") != null && device.currentValue("variable1") != ""){
        data = data + device.currentValue("variable1") + "<br>"
    }
    if(device.currentValue("variable2") != null && device.currentValue("variable2") != ""){
        data = data + device.currentValue("variable2") + "<br>"
    }
    if(device.currentValue("variable3") != null && device.currentValue("variable3") != ""){
        data = data + device.currentValue("variable3") + "<br>"
    }
    if(device.currentValue("variable4") != null && device.currentValue("variable4") != ""){
        data = data + device.currentValue("variable4")
    }
    sendEvent(name:"combined", value: data)
}

def setVariable1(data){
	setVariable("variable1",data)
}

def setVariable2(data){
	setVariable("variable2",data)
}

def setVariable3(data){
	setVariable("variable3",data)
}

def setVariable4(data){
	setVariable("variable4",data)
}