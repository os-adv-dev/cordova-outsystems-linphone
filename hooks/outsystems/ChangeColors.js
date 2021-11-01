
var fs = require('fs');
var path = require('path');
const { getWwwPath } = require('../utils');

var constants = {
    pluginColorPath:"plugins/cordova-outsystems-linphone/src/colors.json",
    android:{
        colorPath:"platforms/android/app/src/main/res/values/colors.xml"
    },
    ios:{
        currentColorPath:"plugins/cordova-outsystems-linphone/src/ios/Colors",
        colorPath:"platforms/ios/$appName/Images.xcassets"
    },
    colorFile:"colors.json"
}

function getAppName(context) {
    var ConfigParser = context.requireCordovaModule("cordova-lib").configparser;
    var config = new ConfigParser("config.xml");
    return config.name();
}

function hexToRgbA(hex){
    var c;
    if(/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)){
        c= hex.substring(1).split('');
        if(c.length== 3){
            c= [c[0], c[0], c[1], c[1], c[2], c[2]];
        }
        c= '0x'+c.join('');
        return [(c>>16)&255, (c>>8)&255, c&255];
    }
    throw new Error('Bad Hex');
}

module.exports = function (context) {
    
    console.log("Start changing Color File!");
    var Q = require("q");
    var deferral = new Q.defer();
 
    var platform = context.opts.plugin.platform;

    var projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    var newColorPath = path.join(projectRoot,constants.pluginColorPath);

    if(fs.existsSync(path.join(projectRoot,"www",constants.colorFile))){
        newColorPath = path.join(projectRoot,"www",constants.colorFile);
    }

    var newContent = JSON.parse(fs.readFileSync(newColorPath, "utf8"));
    
    if(platform == "android"){
        var colorPath = path.join(projectRoot,constants.android.colorPath);
        if (fs.existsSync(colorPath)) {
            var content = fs.readFileSync(colorPath, "utf8");
    
            content = content.replace(new RegExp("([\s|\S]*)(<\/resources>)","g"),(m,m1,m2)=>{
                return m1;
            });

            Object.keys(newContent).forEach(key =>{
                content = content + "\n\t<color name=\""+key+"\">"+newContent[key]+"</color>";
            })
    
            content = content+"\n</resources>";
            
            fs.writeFileSync(colorPath, content);
            console.log("Finished changing color file!");
        }else{
    
            var content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>"
            

            Object.keys(newContent).forEach(key =>{
                content = content + "\n\t<color name=\""+key+"\">"+newContent[key]+"</color>";
            })
    
            content = content+"\n</resources>";
            fs.writeFileSync(colorPath, content);
            console.log("Finished changing color file!");
        }
        deferral.resolve();
    }else if(platform == "ios"){
        constants.ios.currentColorPath = constants.ios.currentColorPath.replace("$appName",getAppName(context));
        constants.ios.colorPath = constants.ios.colorPath.replace("$appName",getAppName(context));
        Object.keys(newContent).forEach(key =>{
            var filePath = path.join(constants.ios.currentColorPath,key+".colorset","Contents.json");
            var futurePath = path.join(constants.ios.colorPath,key+".colorset");
            if (!fs.existsSync(futurePath)){
                fs.mkdirSync(futurePath)
            }
            futurePath = path.join(futurePath,"Contents.json")
            switch(key){
                case "CallBottomButtonBarColor":
                case "headerColor":
                case "HeaderLableColor":
                case "noActiveCallBgColor":
                case "textColor":
                case "textColorNoActiveCall":
                    var content = fs.readFileSync(filePath, "utf8");
                    contentJSON = JSON.parse(content);

                    var rgb = hexToRgbA(newContent[key])

                    contentJSON.colors[0].color.components.red = rgb[0]/255
                    contentJSON.colors[0].color.components.blue = rgb[1]/255
                    contentJSON.colors[0].color.components.green = rgb[2]/255

                    fs.writeFileSync(futurePath, content);
                    break;
            }
        })
        console.log("Finished changing color file!");
        deferral.resolve();
    }
    return deferral.promise;
}