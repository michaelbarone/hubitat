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
 *    Date        Who            What
 *    ----        ---            ----
 * 	 6-11-21	mbarone			initial release 
 */

preferences {
        input("src", "text", title: "iFrame Url",  required: true)
        input("openText", "text", title: "Button text to Open iFrame", defaultValue:"Show",  required: false)
        input("closeText", "text", title: "Button text to close iFrame", defaultValue:"Close", required: false)
    }
metadata {
    definition (name: "iFrameAdvanced", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/iFrameAdvanced.groovy") {
        capability "Actuator"
        attribute "iFrame", "text"
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
        sendEvent(name: "iFrame", value: "<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='block';>${openText}</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display: none;position: fixed;top: 0;left: 0;width: 100%;height: 100%;z-index:100;background-color: rgba(0,0,0,.85);'><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src=${src}; style='position:fixed;top:5px;right:35px;'>Refresh</button><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'; style='position:fixed;top:5px;right:5px;'>${closeText}</button><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src=${src} style='height: 100%; width:100%; border: none;'></iframe></div>")
    }
}