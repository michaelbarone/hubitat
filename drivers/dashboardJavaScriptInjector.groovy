/**
 *  Dashboard JavaScript Injector
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
 *     2   add the javascript file to your hub files area, or any other accessible web server
 *     3   add the url to your javascript file from step 2 and add to the preferences of this device and save the preferences.
 *     4   add this device to your dashboard, select the attribute and choose the "JavaScript" attribute
 *
 *	   optional, but recommended:
 *     4   update the dashboard custom css to hide this tile on the dashboard:
 *
 *		// replace '#tile-33' with this driver/device on your dashboard
 *	
 *		#tile-33 {
 *			display:none;
 *		}
 *
 *      After loading the dashboard, the javascript file should get appended to the bottom of the dashboard iframe, and your script should be available on the page.
 *
 *      each javascript injector device should only be included 1 time for each dashboard.  If you want multiple javascript files to load, create multiple injector devices
 *
 *
 *  Change History:
 *
 *    Date            Who            What
 *    ----            ---            ----
 * 	 6-25-2021  	mbarone			initial release 
 */

preferences {
        input("src", "text", title: "JavaScript url",  required: true)
        input("delayLoad", "int", title: "Delay after Dashboard loads before injecting and running the JavaScript file in milliseconds", defaultValue: 500, required: true)
    }
metadata {
    definition (name: "Dashboard JavaScript Injector", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/dashboardJavaScriptInjector.groovy") {
        capability "Actuator"
        attribute "JavaScript", "text"
    }
}
def installed() {
    setJavaScript()
}
def updated() {
    setJavaScript()
}
def setJavaScript() {
    if(src && src != ""){
        String dn = "${device.displayName.replaceAll('\\s','')}"

        String myScript = '''<img src onerror='
                            function loadScript(){
                                var body = document.getElementsByTagName("body")[0];
                                var script = document.getElementById("'''+dn+'''");
                                var hasScript = script != null;
                                if(!hasScript) {
                                    script = document.createElement("script");
                                    script.setAttribute("id", "'''+dn+'''");
                                }
                                script.type = "text/javascript";
                                script.src = "'''+src+'''";
                                if(!hasScript) {
                                    body.appendChild(script);
                                }
                            }
                            setTimeout(loadScript, '''+delayLoad+''');
                            '></img>'''

        sendEvent(name: "JavaScript", value: "${myScript}", isStateChange: true)
    } else {
        log.warn "No JavaScript path set.  Set JavaScript Url in device preferences."
    }
}