
var fs = require('fs');
var path = require('path');

module.exports = function (context) {
    
    console.log("Start changing CallActivity!");
    var Q = require("q");
    var deferral = new Q.defer();


    var rawConfig = fs.readFileSync("config.xml", 'ascii');
    var match = /^<widget[\s|\S]* id="([\S]+)".+?>$/gm.exec(rawConfig);
    if(!match || match.length != 2){
        throw new Error("id parse failed");
    }

    var id = match[1];
    var appId = id;
    

    var projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    var appFilePath = path.join(projectRoot,"platforms","android","app","src","main","java","com","outsystems","linphone","CallActivity.java");
    if (fs.existsSync(appFilePath)) {
        var content = fs.readFileSync(appFilePath, "utf8");

        var regexAppId = new RegExp("\\$appid","g");
        content = content.replace(regexAppId,appId);

        
        fs.writeFileSync(appFilePath, content);
        console.log("Finished changing CallActivity!");
    }else{
        console.log("Error could not find CallActivity!");
    }
    deferral.resolve();

    return deferral.promise;
}