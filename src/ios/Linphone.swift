//
//  ViewController.swift
//  Wtest
//
//  Created by Danmei Chen on 20/05/2019.
//  Copyright © 2019 belledonne. All rights reserved.
//

import Foundation
import linphonesw
import PushKit

var login: Bool = false
@objc(Linphone) class Linphone: CDVPlugin,CoreDelegate,LoggingServiceDelegate,IncomingCallViewDelegate {
    func incomingCallAccepted(_ call: OpaquePointer!, evenWithVideo video: Bool) {
        CallManager.instance().acceptCall(call: call, hasVideo: video)
    }
    
    func incomingCallDeclined(_ call: OpaquePointer!) {
        CallManager.instance().terminateCall(call: call)
    }
    
    func incomingCallAborted(_ call: OpaquePointer!) {
    }
    
    
    var lc: Core!
    var proxy_cfg: ProxyConfig!
    var call: Call!
    var mIterateTimer: Timer?
    var mCallbackListener: String = ""
    var proxyServer: String = ""
    var callIncomingView: CallIncomingView!
    var callView: CallView!
    var callOutgoingView: CallOutgoingView!
    var restConfig: String = ""
    var dmftConfig: String = ""
        
    override func pluginInitialize() {
        
        if (lc != nil) {
            print("linphonecore is already created!");
            return;
        }
        
        lc = try! Factory.Instance.createCore(configPath: "", factoryConfigPath: "", systemContext: nil)
        
        //lc.pushNotificationEnabled = true
        lc.videoCaptureEnabled = true;
        lc.videoDisplayEnabled = true;
        
        try! lc.start()
        
        
        CallManager.instance().setCore(core:lc.getCobject!);
        
        lc.addDelegate(delegate: self)
        


        /* Set audio assets
        NSString *ring =
            ([LinphoneManager bundleFile:[self lpConfigStringForKey:@"local_ring" inSection:@"sound"].lastPathComponent]
             ?: [LinphoneManager bundleFile:@"notes_of_the_optimistic.caf"])
            .lastPathComponent;
        NSString *ringback =
            ([LinphoneManager bundleFile:[self lpConfigStringForKey:@"remote_ring" inSection:@"sound"].lastPathComponent]
             ?: [LinphoneManager bundleFile:@"ringback.wav"])
            .lastPathComponent;
        NSString *hold =
            ([LinphoneManager bundleFile:[self lpConfigStringForKey:@"hold_music" inSection:@"sound"].lastPathComponent]
             ?: [LinphoneManager bundleFile:@"hold.mkv"])
            .lastPathComponent;
        [self lpConfigSetString:[LinphoneManager bundleFile:ring] forKey:@"local_ring" inSection:@"sound"];
        [self lpConfigSetString:[LinphoneManager bundleFile:ringback] forKey:@"remote_ring" inSection:@"sound"];
        [self lpConfigSetString:[LinphoneManager bundleFile:hold] forKey:@"hold_music" inSection:@"sound"];

        
        [LinphoneManager.instance startLinphoneCore];

        // Load plugins if available in the linphone SDK - otherwise these calls will do nothing
        MSFactory *f = linphone_core_get_ms_factory(theLinphoneCore);
        libmssilk_init(f);
        libmsamr_init(f);
        libmsx264_init(f);
        libmsopenh264_init(f);
        libmswebrtc_init(f);
        libmscodec2_init(f);

        linphone_core_reload_ms_plugins(theLinphoneCore, NULL);
        [self migrationAllPost];

        /* Use the rootca from framework, which is already set*/
        //linphone_core_set_root_ca(theLinphoneCore, [LinphoneManager bundleFile:@"rootca.pem"].UTF8String);
        linphone_core_set_user_certificates_path(theLinphoneCore, linphone_factory_get_data_dir(linphone_factory_get(), kLinphoneMsgNotificationAppGroupId.UTF8String));

        /* The core will call the linphone_iphone_configuring_status_changed callback when the remote provisioning is loaded
           (or skipped).
           Wait for this to finish the code configuration */

        
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        BOOL bAudioInputAvailable = audioSession.inputAvailable;
        NSError *err = nil;

        if (![audioSession setActive:NO error:&err] && err) {
            //LOGE(@"audioSession setActive failed: %@", [err description]);
            err = nil;
        }
        if (!bAudioInputAvailable) {
            UIAlertController *errView = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"No microphone", nil)
                              message:NSLocalizedString(@"You need to plug a microphone to your device to use the application.", nil)
                              preferredStyle:UIAlertControllerStyleAlert];

            UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", nil)
                            style:UIAlertActionStyleDefault
                            handler:^(UIAlertAction * action) {}];

            [errView addAction:defaultAction];
            //[PhoneMainView.instance presentViewController:errView animated:YES completion:nil];
        }

        if ([UIApplication sharedApplication].applicationState == UIApplicationStateBackground) {
            // go directly to bg mode
            [self enterBackgroundMode];
        }*/
    }
    
    @objc(connect:)func connect(command : CDVInvokedUrlCommand){
        #if DEBUG_LOGS
        let log = LoggingService.Instance /*enable liblinphone logs.*/
        log.addDelegate(delegate: self)
        #endif
        
        let username = command.argument(at: 0) as! String
        let userid = command.argument(at: 1)
        let password = command.argument(at: 2) as! String
        let ha1 = command.argument(at: 3)
        let realm = command.argument(at: 4)
        let domain = command.argument(at: 5) as! String
        let algorithm = command.argument(at: 6)
        let proxy = command.argument(at: 7) as! String
        let transportTypeString = command.argument(at: 8) as! String
        
        var transportType:TransportType
        switch transportTypeString {
        case "TCP":
            transportType = TransportType.Tcp;
        case "UDP":
            transportType = TransportType.Udp;
        default:
            transportType = TransportType.Tls;
        }
        
        
        let factory = Factory.Instance
        do {
            //lc.addDelegate(delegate: self)
            //try! lc.start()
            /*create proxy config*/
            //proxy_cfg = try lc.createProxyConfig()
            
            let authInfo = try factory.createAuthInfo(username: username, userid: "", passwd: password, ha1: "", realm: "", domain: domain) /*create authentication structure from identity*/
            
            let params = try lc.createAccountParams()
            
            /*parse identity*/
            let identity = try factory.createAddress(addr: "sip:"+username+"@"+domain)
            
            try params.setIdentityaddress(newValue: identity)
            
            proxyServer = proxy;
            
            let address = try factory.createAddress(addr: "sip:"+proxy+";transport="+transportTypeString.lowercased())
            try address.setTransport(newValue: transportType)
            try params.setServeraddress(newValue: address)
            params.registerEnabled = true;
            //params.pushNotificationAllowed = true;
            //params.remotePushNotificationAllowed = true;
            
            lc.addAuthInfo(info: authInfo)
            
            let account = try lc.createAccount(params: params)
            try lc.addAccount(account: account)
            lc.defaultAccount = account
            
            login = true
            
            /* main loop for receiving notifications and doing background linphonecore work: */
        } catch {
            print(error)
        }
    }
    @objc(setCustomButton1:)func setCustomButton1(command : CDVInvokedUrlCommand){
        restConfig = command.argument(at: 0) as! String;
    }
    @objc(setCustomButton2:)func setCustomButton2(command : CDVInvokedUrlCommand){
        dmftConfig = command.argument(at: 0) as! String;
    }
    
    @objc(disconnect:)func disconnect(command : CDVInvokedUrlCommand){
        if (login) {
            // Here we will disable the registration of our Account
            let account = lc.defaultAccount
            if account == nil {
                return
            }
            let params = account?.params
            
            let clonedParams = params?.clone()
            
            clonedParams?.registerEnabled = false;
            
            account?.params = clonedParams
            /*
            proxy_cfg.edit() /*start editing proxy configuration*/
            proxy_cfg.registerEnabled = false /*de-activate registration for this proxy config*/
            do {
                try proxy_cfg.done()
            } catch {
                print(error)
            } /*initiate REGISTER with expire = 0*/
            
            while(proxy_cfg!.state !=  RegistrationState.Cleared){
                lc.iterate() /*to make sure we receive call backs before shutting down*/
                usleep(50000)
            }*/
        }
    }
    
    @objc(delete:)func delete(command : CDVInvokedUrlCommand){
        // To completely remove an Account
        let account = lc.defaultAccount;
        if (account == nil) {
            return;
        }
        lc.removeAccount(account: account!)

        // To remove all accounts use
        lc.clearAccounts();

        // Same for auth info
        lc.clearAllAuthInfo();
    }

    @objc(setListeners:)func setListeners(command : CDVInvokedUrlCommand){
        mCallbackListener = command.callbackId
    }
    
    @objc(call:)func call(command : CDVInvokedUrlCommand){
        #if DEBUG_LOGS
        let log = LoggingService.Instance /*enable liblinphone logs.*/
        log.addDelegate(delegate: self)
        log.logLevel = LogLevel.Debug
        Factory.Instance.enableLogCollection(state: LogCollectionState.Enabled)
        #endif
        
        var domain = command.argument(at: 0) as! String
        let isVideoNumber = command.argument(at: 1) as! NSNumber
        let isVideo = isVideoNumber.boolValue
        let isLowBandwidth = command.argument(at: 2) as! Bool
        
        if proxyServer != "" {
            domain = domain.components(separatedBy: "@")[0]
            domain = domain + "@" + proxyServer
        }
        let remoteAddress = try? Factory.Instance.createAddress(addr: "sip:"+domain)
        
        if remoteAddress == nil {
            return;
        }
        if lc.defaultAccount != nil {
            let localAddr = lc.defaultAccount?.contactAddress
            if localAddr != nil {
                try! remoteAddress?.setTransport(newValue: localAddr!.transport)
            }
        }
        
        // For OutgoingCall, show CallOutgoingView
        if lc != nil {
            CallManager.instance().setCore(core: lc.getCobject!)
            try! CallManager.instance().doCall(addr: remoteAddress!, isSas: false, isLowBandwidth:isLowBandwidth, isVideo:isVideo)
            
            callOutgoingView = CallOutgoingView()
            viewController.present(callOutgoingView, animated: true, completion: nil)
        }else{
            print("Core not initialized!")
        }
    }
    
    @objc(setAutoAcceptVideo:)func setAutoAcceptVideo(command:CDVInvokedUrlCommand){
        
        lc.videoActivationPolicy?.automaticallyAccept = (command.argument(at: 0) as! NSNumber ).boolValue
    }
    
    @objc(hangup:)func hangup(command : CDVInvokedUrlCommand) {
        if (self.call != nil && self.call!.state != Call.State.End){
            /* terminate the call */
            print("Terminating the call...\n")
            do {
                try self.call?.terminate()
            } catch {
                print(error)
            }
        }
        self.lc.stop()
    }
    
    func onRegistrationStateChanged(core: Core, proxyConfig: ProxyConfig, state: RegistrationState, message: String) {
        let stateString:String
        switch state {
        case .Ok:
            stateString = "OK"
        case .Cleared:
            stateString = "Cleared"
        case .Failed:
            stateString = "Failed"
        case .None:
            stateString = "None"
        case .Progress:
            stateString = "Progress"
        }
        let json = ["Type":"Registration","Status":stateString,"Message":message]
        let result = CDVPluginResult.init(status: CDVCommandStatus_OK, messageAs: json)
        if mCallbackListener != "" {
            self.commandDelegate.send(result, callbackId: mCallbackListener)
        }
        print("New registration state \(state) for user id \( String(describing: proxyConfig.identityAddress?.asString()))\n")
    }
    
    func onAudioDevicesListUpdated(core: Core) {
        let json = ["Type":"Registration","Status":"AudioUpdated"]
        let result = CDVPluginResult.init(status: CDVCommandStatus_OK, messageAs: json)
        if mCallbackListener != "" {
            self.commandDelegate.send(result, callbackId: mCallbackListener)
        }
    }
    
    func onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
        let json = ["Type":"Registration","Status":"AudioChanged"]
        let result = CDVPluginResult.init(status: CDVCommandStatus_OK, messageAs: json)
        if mCallbackListener != "" {
            self.commandDelegate.send(result, callbackId: mCallbackListener)
        }
    }
    
    func onCallStateChanged(core: Core, call: Call, state: Call.State, message: String) {
        let stateString:String
        switch state {
        case .OutgoingRinging:
            stateString = "OutgoingRinging"
        case .OutgoingEarlyMedia:
            stateString = "OutgoingEarlyMedia"
        case .Connected:
            stateString = "Connected"
        case .StreamsRunning:
            stateString = "StreamsRunning"
            if(callView == nil){
                if callIncomingView != nil {
                    callIncomingView.dismiss(animated: true, completion: nil)
                    callIncomingView = nil
                }else if callOutgoingView != nil{
                    callOutgoingView.dismiss(animated: true, completion: nil)
                    callOutgoingView = nil
                }
                callView = CallView()
                callView.dmftConfiguration = dmftConfig
                callView.restConfiguration = restConfig
                viewController.present(callView, animated: true, completion: nil)
            }
        case .End:
            stateString = "End"
            if callIncomingView != nil {
                callIncomingView.dismiss(animated: true, completion: nil)
                callIncomingView = nil
            }else if callView != nil{
                callView.dismiss(animated: true, completion: nil)
                callView = nil
            }else if callOutgoingView != nil{
                callOutgoingView.dismiss(animated: true, completion: nil)
                callOutgoingView = nil
            }
        case .Error:
            stateString = "Error"
            if callIncomingView != nil {
                callIncomingView.dismiss(animated: true, completion: nil)
                callIncomingView = nil
            }else if callView != nil{
                callView.dismiss(animated: true, completion: nil)
                callView = nil
            }else if callOutgoingView != nil{
                callOutgoingView.dismiss(animated: true, completion: nil)
                callOutgoingView = nil
            }
        case .IncomingReceived:
            stateString = "IncomingReceived"
            callIncomingView = CallIncomingView()
            callIncomingView.delegate = self
            callIncomingView.call = core.currentCall?.getCobject
            viewController.present(callIncomingView, animated: true, completion: nil)
        case .EarlyUpdatedByRemote:
            stateString = "EarlyUpdatedByRemote"
        case .EarlyUpdating:
            stateString = "EarlyUpdating"
        case .Idle:
            stateString = "Idle"
        case .IncomingEarlyMedia:
            stateString = "IncomingEarlyMedia"
        case .OutgoingInit:
            stateString = "OutgoingInit"
        case .Updating:
            stateString = "Updating"
        case .UpdatedByRemote:
            stateString = "UpdatedByRemote"
        case .Resuming:
            stateString = "Resuming"
        case .Released:
            stateString = "Released"
        case .Referred:
            stateString = "Referred"
        case .PushIncomingReceived:
            stateString = "PushIncomingReceived"
        case .Pausing:
            stateString = "Pausing"
        case .Paused:
            stateString = "Paused"
        case .PausedByRemote:
            stateString = "PausedByRemote"
        case .OutgoingProgress:
            stateString = "OutgoingProgress"
        }
        let json = ["Type":"Registration","Status":stateString,"Message":message]
        let result = CDVPluginResult.init(status: CDVCommandStatus_OK, messageAs: json)
        if mCallbackListener != "" {
            self.commandDelegate.send(result, callbackId: mCallbackListener)
        }
    }
    
}
