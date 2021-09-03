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
@objc(Linphone) class Linphone: CDVPlugin,CoreDelegate,LoggingServiceDelegate {
    
    var lc: Core!
    var proxy_cfg: ProxyConfig!
    var call: Call!
    var mIterateTimer: Timer?
        
    override func pluginInitialize() {
        do{
        lc = try Factory.Instance.createCore(configPath: "", factoryConfigPath: "", systemContext: nil)
        }catch{
            print(error)
        }
    }
    
    
    @objc func iterate() {
        lc.iterate()
    }

    func startIterateTimer() {
        if (mIterateTimer?.isValid ?? false) {
            print("Iterate timer is already started, skipping ...")
            return
        }
        mIterateTimer = Timer.scheduledTimer(timeInterval: 0.02, target: self, selector: #selector(self.iterate), userInfo: nil, repeats: true)
        print("start iterate timer")

    }

    func stopIterateTimer() {
        if let timer = mIterateTimer {
            print("stop iterate timer")
            timer.invalidate()
        }
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
        
        
        let factory = Factory.Instance
        do {
            lc.addDelegate(delegate: self)
            try! lc.start()
            /*create proxy config*/
            proxy_cfg = try lc.createProxyConfig()
            /*parse identity*/
            let from = try factory.createAddress(addr: username)
            if (password != nil){
                let info = try factory.createAuthInfo(username: from.username, userid: "", passwd: password, ha1: "", realm: "", domain: domain) /*create authentication structure from identity*/
                lc!.addAuthInfo(info: info) /*add authentication info to LinphoneCore*/
            }
            // configure proxy entries
            try proxy_cfg.setIdentityaddress(newValue: from) /*set identity with user name and domain*/
            try proxy_cfg.setServeraddr(newValue: proxy) /* we assume domain = proxy server address*/
            proxy_cfg.registerEnabled = true /*activate registration for this proxy config*/
            
            try lc.addProxyConfig(config: proxy_cfg!) /*add proxy config to linphone core*/
            lc.defaultProxyConfig = proxy_cfg /*set to default proxy*/
            
            login = true
            /* main loop for receiving notifications and doing background linphonecore work: */
            startIterateTimer()
        } catch {
            print(error)
        }
    }
    
    @objc(disconnect:)func disconnect(command : CDVInvokedUrlCommand){
        if (login) {
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
            }

            stopIterateTimer()
            lc.removeDelegate(delegate: self)
            lc.stop()
        }
    }
    @objc(call:)func call(command : CDVInvokedUrlCommand){
        #if DEBUG_LOGS
        let log = LoggingService.Instance /*enable liblinphone logs.*/
        log.addDelegate(delegate: self)
        log.logLevel = LogLevel.Debug
        Factory.Instance.enableLogCollection(state: LogCollectionState.Enabled)
        #endif
        
        let dest = command.argument(at: 0) as! String
        try! lc.start()
            
        if (dest != nil){
            /*
            Place an outgoing call
            */
            call = lc.invite(url: dest)
            if (call == nil) {
                print("Could not place call to \(dest ?? "")\n")
            } else {
                print("Call to  \(dest ?? "") is in progress...")
            }
        }
        
        startIterateTimer()
    }
    
    @objc(hangup:)func hangup(command : CDVInvokedUrlCommand) {
        stopIterateTimer()
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
    
    func onRegistrationStateChanged(lc: Core, cfg: ProxyConfig, cstate: RegistrationState, message: String) {
        print("New registration state \(cstate) for user id \( String(describing: cfg.identityAddress?.asString()))\n")
    }
    func onLogMessageWritten(logService: LoggingService, domain: String, lev: LogLevel, message: String) {
        print("Logging service log: \(message)s\n")
    }
    func onCallStateChanged(lc: Core, call: Call, cstate: Call.State, message: String) {
        switch cstate {
        case .OutgoingRinging:
            print("It is now ringing remotely !\n")
        case .OutgoingEarlyMedia:
            print("Receiving some early media\n")
        case .Connected:
            print("We are connected !\n")
        case .StreamsRunning:
            print("Media streams established !\n")
        case .End:
            print("Call is terminated.\n")
        case .Error:
            print("Call failure !")
        default:
            print("Unhandled notification \(cstate)\n")
        }
    }
    
}
