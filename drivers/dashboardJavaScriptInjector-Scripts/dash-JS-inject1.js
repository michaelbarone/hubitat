(function() {
    function pageStart(){
        //console.log("pageStart called");

        if(window.location.pathname.includes("device/edit")){
            // log to console that the script was injected properly, but stopped becuase we are on the device page
            console.log("JavaScript has been injected!  The script was stopped because it was injected into the device/edit page.  This should work on a dashboard.")
            // stop execution if we are on the device edit page, and not a dashboard.
            return;
        }
        /************************************/
        /* Add custom JavaScript below here */
        /************************************/



        
        
        /* remove specific words from tile titles */
        var words = ['All','Ave','Temp'];
        var expStr = words.join("|");
        var el = document.getElementsByClassName("tile-title");
        if(el.length>0){
            for(item of el){
                if(item.innerHTML.length>0){
                    var temp = item.innerHTML.replace(new RegExp('\\b(' + expStr + ')\\b', 'gi'), ' ').replace(/\s{2,}/g, ' ');
                    item.innerHTML = temp;
                }
            }
        }

       

        //console.log("pageStart ended");
    }
    pageStart();
})();








