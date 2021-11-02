var exec = require("cordova/exec");

exports.setListeners = function(success,failure){
    exec(success,failure,"Linphone","setListeners",[])
}
exports.setCustomButton1 = function(success,failure,jsonstring){
    exec(success,failure,"Linphone","setCustomButton1",[jsonstring])
}
exports.setCustomButton2 = function(success,failure,jsonstring){
    exec(success,failure,"Linphone","setCustomButton2",[jsonstring])
}

exports.connect = function(success,failure,username,password,domain,proxy,transportType,userid,ha1,realm,algorithm){
    exec(success,failure,"Linphone","connect",[username,userid,password,ha1,realm,domain,algorithm,proxy,transportType])
}
exports.disconnect = function(success,failure){
    exec(success,failure,"Linphone","disconnect",[])
}
exports.delete = function(success,failure){
    exec(success,failure,"Linphone","delete",[])
}
exports.call = function(success,failure,domain,isVideo,lowBandwidth,earlyMedia){
    exec(success,failure,"Linphone","call",[domain,isVideo,lowBandwidth,earlyMedia])
}
exports.setAutoAcceptVideo = function(success,failure,boolvalue){
    exec(success,failure,"Linphone","setAutoAcceptVideo",[domain,boolvalue])
}