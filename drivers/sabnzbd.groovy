/**
 *  Sabnzbd Device Driver for Hubitat
 *  michaelbarone
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
 */

def setVersion(){
	state.version = "1.0.1"
}

metadata {
    definition (name: "sabnzbd", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/sabnzbd.groovy") {
        capability "Actuator"
		capability "Initialize"
        //capability "Switch"
        capability "PresenceSensor"
        
		attribute "diskspacetotal1", "String"
		attribute "diskspace1", "String"
		attribute "diskspacetotal2", "String"
		attribute "diskspace2", "String"
		attribute "have_warnings", "String"
		attribute "last_warning", "String"
		attribute "speedlimit", "String"
		attribute "speed", "String"
		attribute "status", "String"
		attribute "uptime", "String"
		attribute "finish", "String"
		attribute "mb", "String"
		attribute "mbleft", "String"
		attribute "timeleft", "String"
		attribute "lastSabnzbdCheck", "String"
		attribute "lastSabnzbdUpdate", "String"

        
        command "CheckSabnzbd", null
    }

    preferences {
        section("Device Settings:") {
            input "ip_addr", "string", title:"ip address", description: "", required: true, displayDuringSetup: true
            input "url_port", "string", title:"tcp port", description: "", required: true, displayDuringSetup: true, defaultValue: "80"
            input "api_key", "string", title:"API Key", description: "", required: true, displayDuringSetup: true, defaultValue: ""
            
            input "delayCheck", "number", title:"Number of minutes between checking sabnzbd api", description: "", required: true, displayDuringSetup: true, defaultValue: "600"
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
    }


}        
void parse(String toparse){
    if (logEnable) log.info "Parsing: ${toparse}"
}

void initialize(){
	unschedule()
    if (autoUpdate) runIn(1, CheckSabnzbd)
}

def updated(){
	unschedule()
    if (autoUpdate) runIn(5, CheckSabnzbd)
}

def CheckSabnzbd() {
	unschedule(CheckSabnzbd)
	GetSabnzbd()
}

def GetSabnzbd() {
	
	def nowDay = new Date().format("MMM dd", location.timeZone)
	def nowTime = new Date().format("h:mm:ss a", location.timeZone)
	sendEvent(name: "lastSabnzbdCheck", value: nowDay + " at " + nowTime, displayed: false)	
    
    def wxURI2 = "http://${ip_addr}:${url_port}/sabnzbd/api?mode=queue&output=json&apikey=${api_key}"
    def toReturn = " "
        
    def requestParams2 =
	[
		uri:  wxURI2,
        headers: [ 
                   "User-Agent": "Wget/1.20.1",
                   Accept: "*/*",
                   "Accept-Encoding": "identity",
                   Host: "${ip_addr}",
                   Connection: "Keep-Alive",
                   "X-Api-Key": "${api_key}",
                 ],
	]
    try{
		httpGet(requestParams2)
		{
		  response ->
		  log.debug response.status
		  log.debug response.data
		  
		  def wantedStates = ["diskspacetotal1","diskspace1","diskspacetotal2","diskspace2","have_warnings","last_warning","speedlimit","speed","status","uptime","finish","mb","mbleft","timeleft"]

			if (response?.status == 200){
				sendEvent(name: "lastSabnzbdUpdate", value: nowDay + " at " + nowTime, displayed: false)	
				wantedStates.each{
					log.debug it
					log.debug response.data.queue[it].value
					
					if (response.data.queue[it].value != null)
						{
							sendEvent(name: it, value: response.data.queue[it].value.toString())
						} else {
							sendEvent(name: it, value: 0 )
						}
				}
				toReturn = response.data.toString()
				sendEvent(name: "stateMessage", value: "none")
			}
			else
			{
				log.warn "${response?.status}"
				sendEvent(name: "stateMessage", value: "${response?.status}")
				// set default status for values
				SabnzbdNotResponding()
			}
			sendEvent(name: "presence", value: "present")
		}
    } catch (Exception e){
        log.info e
        toReturn = e.toString()
		sendEvent(name: "stateMessage", value: toReturn)

		// set default status for values
		SabnzbdNotResponding()
    }

	unschedule(CheckSabnzbd)
	runIn(delayCheck.toInteger()*60, CheckSabnzbd)
    return toReturn
}

def SabnzbdNotResponding(){
	sendEvent(name: "presence", value: "not present")
}