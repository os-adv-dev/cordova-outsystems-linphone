
var fs = require('fs');
var path = require('path');

module.exports = function (context) {
    
    console.log("Start changing Code Files!");
    var Q = require("q");
    var deferral = new Q.defer();


    var ConfigParser = require("cordova-common").ConfigParser;
    var appConfig = new ConfigParser('config.xml');
    projectName = appConfig.name();
    

    var projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    var pluginFilesPath = path.join(projectRoot,"platforms","ios",projectName,"Plugins","cordova-outsystems-linphone");

    var callIncomingViewPath = path.join(pluginFilesPath,"CallIncomingView.m");
    var callOutgoingViewPath = path.join(pluginFilesPath,"CallOutgoingView.m");
    var callViewPath = path.join(pluginFilesPath,"CallView.m");
    var uiaudio = path.join(pluginFilesPath,"UILinphoneAudioPlayer.m");
    var utils = path.join(pluginFilesPath,"Utils.h");

    projectName = projectName.replace(new RegExp(" ","g"),"_");

    replaceinFile(callIncomingViewPath,new RegExp("\\$appname","g"),projectName);
    replaceinFile(callOutgoingViewPath,new RegExp("\\$appname","g"),projectName);
    replaceinFile(callViewPath,new RegExp("\\$appname","g"),projectName);
    replaceinFile(utils,new RegExp("\\$appname","g"),projectName);
    replaceinFile(uiaudio,new RegExp("\\$appname","g"),projectName);
    deferral.resolve();

    return deferral.promise;

    function replaceinFile(path,replaceRegex,toReplace){
        if (fs.existsSync(path)) {
            var content = fs.readFileSync(path, "utf8");

            content = content.replace(replaceRegex,toReplace);
    
            fs.writeFileSync(path, content);
            console.log("Finished changing "+path+"!");
        }else{
            console.log("Error could not find "+path+"!");
        }
    }
}