(function() {
    function pageStart(){
        //console.log("pageStart called");
        
        
        
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








