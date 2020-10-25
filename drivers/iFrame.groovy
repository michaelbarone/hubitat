/**
 *  dashboard iFrame
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
 *		#tile-33 .tile-contents, #tile-33 .tile-primary{
 *			padding: 0;
 *			height:100%;
 *		}
 *
 *
 *
 *
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 * 	 10-12-20	mbarone			initial release 
 * 	 10-25-20	mbarone			update to include css and directions 
 */

preferences {
        input("src", "text", title: "iFrame Url",  required: true)
    }
metadata {
    definition (name: "iFrame", namespace: "mbarone", author: "mbarone", importUrl: "https://raw.githubusercontent.com/michaelbarone/hubitat/master/drivers/iFrame.groovy") {
        capability "Actuator"
        attribute "iFrame", "text"
    }
}
def installed() {
	sendEvent(name: "iFrame", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
}
def updated() {
    sendEvent(name: "iFrame", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
}