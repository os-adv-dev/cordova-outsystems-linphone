"use strict";

var path = require("path");
var AdmZip = require("adm-zip");
var fs = require("fs");

var utils = require("../utils");

var constants = {
  pluginId: "cordova-outsystems-linphone",
  imageAssets: "imageAssets",
  platform: "platforms",
  zipExtension: ".zip",
  wwwFolder: "www",
  platforms:{
    android:{
      platform:"android",
      imageFolder: "src/android/drawable"
    },
    ios:{
      platform:"ios",
      imageFolder:"src/ios/images"
    }
  }
};

function getAppName(context) {
  var ConfigParser = context.requireCordovaModule("cordova-lib").configparser;
  var config = new ConfigParser("config.xml");
  return config.name();
}

function getPlatformConfigs(platform) {
  if (platform === constants.platforms.android.platform) {
    return constants.platforms.android;
  } else if (platform === constants.platforms.ios.platform) {
    return constants.platforms.ios;
  }
}
function getZipFile(folder, zipFileName) {
  try {
    var files = fs.readdirSync(folder);
    for (var i = 0; i < files.length; i++) {
      if (files[i].endsWith(constants.zipExtension)) {
        var fileName = path.basename(files[i], constants.zipExtension);
        console.log(fileName)
        console.log(zipFileName)
        if (fileName === zipFileName) {
          return path.join(folder, files[i]);
        }
      }
    }
  } catch (e) {
    console.log(e);
    return;
  }
}
module.exports = function(context) {
  var cordovaAbove8 = utils.isCordovaAbove(context, 8);
  var defer;
  if (cordovaAbove8) {
    defer = require('q').defer();
  } else {
    defer = context.requireCordovaModule("q").defer();
  }
  
  console.log("Start changing images!");
  var platform = context.opts.plugin.platform;

  var platformConfig = getPlatformConfigs(platform);
  if (!platformConfig) {
    console.log("Invalid platform");
    defer.reject();
  }

  var platformPath = path.join(context.opts.projectRoot, constants.platform, platform);
  var wwwPath = path.join(context.opts.projectRoot, constants.wwwFolder);
  
  var imageZipFile = getZipFile(wwwPath, constants.imageAssets);
  if (!imageZipFile) {
    console.log("No zip file found containing image files");
    return;
  }

  var zip = new AdmZip(imageZipFile);

  var targetPath = path.join(wwwPath, constants.imageAssets);
  zip.extractAllTo(targetPath, true);

  targetPath = path.join(targetPath,platform);
  if(!fs.existsSync(targetPath)){
    console.log("No image assets zip found");
    return defer.promise;
  }
  var files = fs.readdirSync(targetPath);
  var destFolder = path.join(context.opts.projectRoot,"plugins",constants.pluginId,constants.platforms[platform].imageFolder);
  
  if (files.length == 0) {
    console.log("No image files found");
    return defer.promise;
  }

  files.forEach(file => {
    var sourcePath = path.resolve(targetPath,path.basename(file));
    var destPath = path.join(destFolder,path.basename(file))
    
    fs.createReadStream(sourcePath,'binary').pipe(fs.createWriteStream(destPath,'binary'))
    .on("close", function (err) {
      defer.resolve();
    })
    .on("error", function (err) {
      console.log("Error changing images!");
      console.log(err);
      defer.reject();
    });
  });
  console.log("Finished changing images!");
  
  return defer.promise;
}