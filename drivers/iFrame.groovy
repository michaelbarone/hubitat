preferences {
        input("src", "text", title: "iFrame Url",  required: true)
    }
metadata {
    definition (name: "iFrame", namespace: "mbarone", author: "mbarone") {
        capability "Switch"
        attribute "myFrame", "text"
    }
}
def on() {
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "myFrame", value: "<div style='height: 100%; width: 100%'><iframe src='${src}' style='height: 100%; width:100%; border: none;'></iframe><div>")
}
def off() {
    sendEvent(name: "switch", value: "off")
}
def installed() {
    on()
}
def updated() {
    on()
}