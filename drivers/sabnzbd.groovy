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
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 * 	 6-9-21		mbarone			initial release 
 * 	 6-9-21		mbarone			resolved nullpointerexception
 */

def setVersion(){
	state.version = "1.0.2"
}

metadata {
    definition (name: "SABnzbd", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/sabnzbd.groovy") {
        capability "Actuator"
		capability "Initialize"
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
		attribute "version", "String"
		attribute "lastSABnzbdCheck", "String"
		attribute "lastSABnzbdUpdate", "String"
		attribute "stateMessage", "String"

        
        command "CheckSABnzbd", null
        command "Pause", null
        command "Resume", null
        command "ClearWarnings", null
        command "PauseDelayResume", [ [ name:"DelayTime*", type:"NUMBER", description:"How many minutes before resumeing after this pause", constraints:[ "NUMBER" ] ] ]
        command "SpeedLimitPercent", [ [ name:"Percent*", type:"NUMBER", description:"Set max download speed to a percent of the KB/s limit || **In SABnzbd 0.7.20 and below the value is always interpreted as KB/s, no percentages.**", constraints:[ "NUMBER" ] ] ]
        command "SpeedLimitKBs", [ [ name:"SpeedLimit*", type:"NUMBER", description:"Set max download speed in KB/s || **In SABnzbd 0.7.20 and below this is not implemented, use the SpeedLimitPercent command**", constraints:[ "NUMBER" ] ] ]

		command "shutdown", null
		command "restart", null
		command "restart_repair", null
		command "pause_pp", null
		command "resume_pp", null
		command "rss_now", null        
		command "watched_now", null
		command "reset_quota", null

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
	unschedule(CheckSABnzbd)
    runIn(1, CheckSABnzbd)
}

def updated(){
	if (logEnable) {
		log.warn "debug logging enabled..."
		runIn(1800,logsOff)
	}	
	unschedule(CheckSABnzbd)
    runIn(3, CheckSABnzbd)
}

void uninstalled(){
	unschedule()
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def CheckSABnzbd() {
	unschedule(CheckSABnzbd)
	GetSABnzbd()
}

def GetSABnzbd() {
	unschedule(CheckSABnzbd)
	runIn(delayCheck.toInteger()*60, CheckSABnzbd)

	
	def nowDay = new Date().format("MMM dd", location.timeZone)
	def nowTime = new Date().format("h:mm:ss a", location.timeZone)
	sendEvent(name: "lastSABnzbdCheck", value: nowDay + " at " + nowTime, displayed: false)	
    
    
	
	//def wxURI2 = "http://${ip_addr}:${url_port}/sabnzbd/api?mode=fullstatus&output=json&apikey=${api_key}"
	
	
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
			if (response?.status == 200){
				def wantedStates = ["diskspacetotal1","diskspace1","diskspacetotal2","diskspace2","have_warnings","last_warning","speedlimit","speed","status","uptime","finish","mb","mbleft","timeleft","version"]
				sendEvent(name: "lastSABnzbdUpdate", value: nowDay + " at " + nowTime, displayed: false)	
				wantedStates.each{
					if (response.data.queue[it]?.value && response.data.queue[it]?.value != null)
						{
							sendEvent(name: it, value: response.data.queue[it].value.toString())
						} else {
							sendEvent(name: it, value: 0 )
						}
				}
				toReturn = response.data.toString()
				sendEvent(name: "stateMessage", value: "none")
				sendEvent(name: "presence", value: "present")
			}
			else
			{
				log.warn "${response?.status}"
				sendEvent(name: "stateMessage", value: "${response?.status}")
				// set default status for values
				SabnzbdNotResponding()
			}
		}
    } catch (Exception e){
        log.info e
        toReturn = e.toString()
		sendEvent(name: "stateMessage", value: toReturn)

		// set default status for values
		SabnzbdNotResponding()
    }
    return toReturn
}

def SabnzbdNotResponding(){
	sendEvent(name: "presence", value: "not present")
}

def SpeedLimitPercent(p){
    SendCommand("mode=config&name=speedlimit&value=${p}")
}
def SpeedLimitKBs(kbs){
    SendCommand("mode=config&name=speedlimit&value=${kbs}K")
}
def PauseDelayResume(time){
    SendCommand("mode=config&name=set_pause&value=${time}")
}
def Resume(){
    SendCommand("mode=resume")
}
def Pause(){
    SendCommand("mode=pause")
}
def ClearWarnings(){
    SendCommand("mode=warnings&name=clear")
}
def shutdown(){
    SendCommand("mode=shutdown")
}
def restart(){
    SendCommand("mode=restart")
}
def restart_repair(){
    SendCommand("mode=restart_repair")
}
def pause_pp(){
    SendCommand("mode=pause_pp")
}
def resume_pp(){
    SendCommand("resume_pp")
}
def rss_now(){
    SendCommand("mode=rss_now")
}
def watched_now(){
    SendCommand("mode=watched_now")
}
def reset_quota(){
    SendCommand("mode=reset_quota")
}

def SendCommand(String payload) {
    def headers = [:] 
    headers.put("HOST", "${ip_addr}:${url_port}")
    headers.put("Content-Type", "application/json")
    headers.put("X-Api-Key", "${api_key}")
    
    try {
        def hubAction = new hubitat.device.HubAction(
            method: "POST",
            path: "/sabnzbd/api?&apikey=${api_key}&${payload}",
            body: "",
            headers: headers
            )
		unschedule(CheckSABnzbd)
		runIn(3, CheckSABnzbd)
        return hubAction
    }
    catch (Exception e) {
        log.debug "runCmd hit exception ${e} on ${hubAction}"
    }
}