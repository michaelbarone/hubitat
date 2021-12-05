/**
 *  dashboard iFrame Advanced
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
 *
 *
 *  Directions:  
 *     1   Create a virtual device with this driver.  
 *     2   add the url you want to embed to the preferences of this device.
 *     3   add this device to your dashboard, select the attribute and choose the "iFrame" attribute
 *
 *	   optional:
 *     4   update the css to style the iframe tile on the dashboard:
 *
 *		// replace '#tile-33' with this driver/device on your dashboard
 *	
 *		#tile-33 .tile-title {
 *			display:none;
 *		}
 *
 *
 *
 *
 *
 *
 *  Change History:
 *
 *    Date            Who            What
 *    ----            ---            ----
 * 	 07-02-2021 	mbarone			initial release 
 * 	 12-05-2021 	mbarone			added autoCloseDelay option and surfaced other modal options in preferences
 */

def setVersion(){
	state.version = "1.0.2"
}

preferences {
        input("src", "text", title: "iFrame Url",  required: true)
        input("openText", "text", title: "Button text to Open iFrame", defaultValue:"Show",  required: false)
        input("closeText", "text", title: "Button text to close iFrame", defaultValue:"Close", required: false)
        input("refreshText", "text", title: "Button text to refresh iFrame content", defaultValue:"Refresh", required: false)
		input("width", "number", title: "Width of iFrame when in view as a percentage (default: 100)", defaultValue:100, required: false)
		input("height", "number", title: "Height of iFrame when in view as a percentage (default: 100)", defaultValue:100, required: false)
        input("delayLoad", "bool", title: "On Demand iFrame Loading", description: "When DISABLED the iFrame will load with the dashboard and remain connected even when not visible.  When ENABLED the iFrame will only load content when visible, it will unload when closed and reload each time you open it.", defaultValue:false,  required: false)
        input("autoCloseDelay", "number", title: "Number of seconds to automatically close the iFrame after opening it (default: 0, 0 = disabled)", description: "This countdown starts when the modal is opened, and cannot be cancelled. It will close the modal at the end of the count down unless it has already been closed", defaultValue:0, required: false)
        input("modalCSS", "text", title: "Style to apply to modal", description: "defaults: position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85);", defaultValue:"position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85);", required: false)
    }
metadata {
    definition (name: "iFrameAdvanced", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/iFrameAdvanced.groovy") {
        capability "Actuator"
        attribute "iFrame", "text"
        attribute "iFrameLauncher", "text"
        attribute "iFrameLauncherCharacterCount", "text"
    }
}
def installed() {
    setIframe()
}
def updated() {
    setIframe()
}
def setIframe() {
    if(src && src != ""){
        sendEvent(name: "iFrame", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")


        def launcher = ""
        if(autoCloseDelay && autoCloseDelay > 0){
            launcher = launcher + "<button style='height:100%;width:100%;' onclick='autoClose = setTimeout(function(){document.getElementById(`${device.displayName.replaceAll('\\s','')}`).style.display=`none`},${autoCloseDelay}000);"
        } else {
            launcher = launcher + "<button style='height:100%;width:100%;' onclick='"
        }


        if(delayLoad){
            launcher = launcher + "document.getElementById(`${device.displayName.replaceAll('\\s','')}`).style.display=`block`;document.getElementById(`${device.displayName.replaceAll('\\s','')}-iframe`).src=`${src}`;'>${openText}</button>"
        } else {
            launcher = launcher + "document.getElementById(`${device.displayName.replaceAll('\\s','')}`).style.display=`block`;'>${openText}</button>"
        }

        launcher = launcher + "<div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;z-index:100;${modalCSS}'>"
        
        if(delayLoad){
            launcher = launcher + "<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none';document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src=''; style='float:right;margin:5px;'>${closeText}</button>"
        } else {
            launcher = launcher + "<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'; style='float:right;margin:5px;'>${closeText}</button>"
        }

        launcher = launcher + "<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:right;margin:5px;'>${refreshText}</button>"

        if(delayLoad){
            launcher = launcher + "<iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=''"
        } else {
            launcher = launcher + "<iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=${src}"
        }
        if(width > 90 && height > 95){
            launcher = launcher + " style='height:${height}%;width:${width}%;border:none;'></iframe></div>"
        } else {
            launcher = launcher + " style='height:${height}%;width:${width}%;border:none;left:0;position:absolute;'></iframe></div>"
        }

        sendEvent(name: "iFrameLauncher", value: launcher)

        def launcherLength = launcher.length()

        def characterCountText = "<text style="
        if(launcherLength > 1024){
            characterCountText = characterCountText + "'color:red'"
        } else if(launcherLength < 980){
            characterCountText = characterCountText + "'color:green'"
        } else {
            characterCountText = characterCountText + "'color:orange'"
        }
        characterCountText = characterCountText + ">${launcherLength}</text>"
        sendEvent(name: "iFrameLauncherCharacterCount", value: characterCountText)
    } else {
        log.warn "No website to embed.  Set iFrame Url in device preferences."
    }
}