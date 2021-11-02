/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-iphone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#import <AddressBook/AddressBook.h>
#import <AudioToolbox/AudioToolbox.h>
#import <OpenGLES/EAGL.h>
#import <OpenGLES/EAGLDrawable.h>
#import <QuartzCore/CAAnimation.h>
#import <QuartzCore/QuartzCore.h>
#import <UserNotifications/UserNotifications.h>

#import "CallView.h"
//#import "PhoneMainView.h"
#import "Utils.h"

#include "linphone/linphonecore.h"

#import "$appname-Swift.h"

const NSInteger SECURE_BUTTON_TAG = 5;

@implementation CallView {
    BOOL hiddenVolume;
}

#pragma mark - Lifecycle Functions

- (id)init {
    self = [super initWithNibName:NSStringFromClass(self.class) bundle:[NSBundle mainBundle]];
    if (self != nil) {
        singleFingerTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(toggleControls:)];
        videoHidden = TRUE;
        [self updateCallView];
    }
    return self;
}

#pragma mark - ViewController Functions

- (void)viewDidLoad {
    [super viewDidLoad];

    _routesEarpieceButton.enabled = YES;

// TODO: fixme! video preview frame is too big compared to openGL preview
// frame, so until this is fixed, temporary disabled it.
#if 0
    _videoPreview.layer.borderColor = UIColor.whiteColor.CGColor;
    _videoPreview.layer.borderWidth = 1;
#endif
    [singleFingerTap setNumberOfTapsRequired:1];
    [singleFingerTap setCancelsTouchesInView:FALSE];
    [self.videoView addGestureRecognizer:singleFingerTap];

    //[videoZoomHandler setup:_videoGroup];
    _videoGroup.alpha = 0;

    [_videoCameraSwitch setPreview:_videoPreview];

    UIPanGestureRecognizer *dragndrop =
        [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(moveVideoPreview:)];
    dragndrop.minimumNumberOfTouches = 1;
    [_videoPreview addGestureRecognizer:dragndrop];

    [_zeroButton setDigit:'0'];
    [_zeroButton setDtmf:true];
    [_oneButton setDigit:'1'];
    [_oneButton setDtmf:true];
    [_twoButton setDigit:'2'];
    [_twoButton setDtmf:true];
    [_threeButton setDigit:'3'];
    [_threeButton setDtmf:true];
    [_fourButton setDigit:'4'];
    [_fourButton setDtmf:true];
    [_fiveButton setDigit:'5'];
    [_fiveButton setDtmf:true];
    [_sixButton setDigit:'6'];
    [_sixButton setDtmf:true];
    [_sevenButton setDigit:'7'];
    [_sevenButton setDtmf:true];
    [_eightButton setDigit:'8'];
    [_eightButton setDtmf:true];
    [_nineButton setDigit:'9'];
    [_nineButton setDtmf:true];
    [_starButton setDigit:'*'];
    [_starButton setDtmf:true];
    [_hashButton setDigit:'#'];
    [_hashButton setDtmf:true];
}

- (void)dealloc {
    // Remove all observer
    [NSNotificationCenter.defaultCenter removeObserver:self];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    _waitView.hidden = TRUE;
    CallManager.instance.nextCallIsTransfer = FALSE;
    
    callRecording = FALSE;

    // Update on show
    [self hideRoutes:TRUE animated:FALSE];
    [self hidePad:TRUE animated:FALSE];
    [self hideSpeaker:[[CallManager instance] isBluetoothAvailable]];
    [self callDurationUpdate];
    [self onCurrentCallChange];
    [[CallManager instance] getFrontCamId];
    // Set windows (warn memory leaks)
    linphone_core_set_native_video_window_id([[CallManager instance] getCore], (__bridge void *)(_videoView));
    linphone_core_set_native_preview_window_id([[CallManager instance] getCore], (__bridge void *)(_videoPreview));

    [self previewTouchLift];
    // Enable tap
    [singleFingerTap setEnabled:TRUE];

    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(messageReceived:)
                                               name:@"LinphoneMessageReceived"
                                             object:nil];
    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(bluetoothAvailabilityUpdateEvent:)
                                               name:@"LinphoneBluetoothAvailabilityUpdate"
                                             object:nil];
    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(callUpdateEvent:)
                                               name:@"LinphoneCallUpdate"
                                             object:nil];

    [NSTimer scheduledTimerWithTimeInterval:1
                                     target:self
                                   selector:@selector(callDurationUpdate)
                                   userInfo:nil
                                    repeats:YES];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];

    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
    [[UIDevice currentDevice] setProximityMonitoringEnabled:TRUE];
    
    hiddenVolume = TRUE;
    
    [[CallManager instance] getFrontCamId];
    // we must wait didAppear to reset fullscreen mode because we cannot change it in viewwillappear
    LinphoneCall *call = linphone_core_get_current_call([[CallManager instance] getCore]);
    LinphoneCallState state = (call != NULL) ? linphone_call_get_state(call) : 0;
    [self callUpdate:call state:state animated:FALSE];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
[[UIDevice currentDevice] setProximityMonitoringEnabled:FALSE];
    [self disableVideoDisplay:TRUE animated:NO];

    if (hideControlsTimer != nil) {
        [hideControlsTimer invalidate];
        hideControlsTimer = nil;
    }

    if (hiddenVolume) {
        //TODO
        //[PhoneMainView.instance setVolumeHidden:FALSE];
        hiddenVolume = FALSE;
    }

    // Remove observer
    [NSNotificationCenter.defaultCenter removeObserver:self];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];

    [[UIApplication sharedApplication] setIdleTimerDisabled:false];
    [[UIDevice currentDevice] setProximityMonitoringEnabled:FALSE];

    // Disable tap
    [singleFingerTap setEnabled:FALSE];

    if (linphone_core_get_calls_nb([[CallManager instance] getCore]) == 0) {
        // reseting speaker button because no more call
        _speakerButton.selected = FALSE;
    }
    /*
    NSString *address = [LinphoneManager.instance lpConfigStringForKey:@"sas_dialog_denied"];
    if (address) {
        UIConfirmationDialog *securityDialog = [UIConfirmationDialog ShowWithMessage:NSLocalizedString(@"Trust has been denied. Make a call to start the authentication process again.", nil)
         cancelMessage:NSLocalizedString(@"CANCEL", nil)
         confirmMessage:NSLocalizedString(@"CALL", nil)
         onCancelClick:^() {
         }
         onConfirmationClick:^() {
             LinphoneAddress *addr = linphone_address_new(address.UTF8String);
             [CallManager.instance startCallWithAddr:addr isSas:TRUE];
             linphone_address_unref(addr);
         } ];
        [securityDialog.securityImage setImage:[UIImage imageNamed:@"security_alert_indicator.png"]];
        securityDialog.securityImage.hidden = FALSE;
        [securityDialog setSpecialColor];
        [LinphoneManager.instance lpConfigSetString:nil forKey:@"sas_dialog_denied"];
    }*/
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
    [self previewTouchLift];
    [self updateCallView];
    LinphoneCall *call = linphone_core_get_current_call([[CallManager instance] getCore]) ;
    if (call && linphone_call_get_state(call) == LinphoneCallStatePausedByRemote) {
        _pausedByRemoteView.hidden = NO;
        [self updateInfoView:TRUE];
    }
    _conferenceView.hidden = !linphone_core_is_in_conference([[CallManager instance] getCore]);
}

#pragma mark - UI modification

- (void)updateInfoView:(BOOL)pausedByRemote {
    CGRect infoFrame = _infoView.frame;
    if (pausedByRemote || !videoHidden) {
        infoFrame.origin.y = 0;
    } else {
        infoFrame.origin.y = (_avatarImage.frame.origin.y-66)/2;
    }
    _infoView.frame = infoFrame;
}

- (void)updateCallView {
    /*CGRect pauseFrame = _callPauseButton.frame;
    if (videoHidden) {
        pauseFrame.origin.y = _bottomBar.frame.origin.y - pauseFrame.size.height - 60;
    } else {
        pauseFrame.origin.y = _videoCameraSwitch.frame.origin.y+_videoGroup.frame.origin.y;
    }
    _callPauseButton.frame = pauseFrame;*/
    [self updateInfoView:FALSE];
}

- (void)hideSpinnerIndicator:(LinphoneCall *)call {
    _videoWaitingForFirstImage.hidden = TRUE;
}

static void hideSpinner(LinphoneCall *call, void *user_data) {
    CallView *thiz = (__bridge CallView *)user_data;
    [thiz hideSpinnerIndicator:call];
}

- (void)updateBottomBar:(LinphoneCall *)call state:(LinphoneCallState)state {
    [_speakerButton update];
    [_microButton update];
    [_callPauseButton update];
    [_conferencePauseButton update];
    [_videoButton update];
    [_hangupButton update];

    switch (state) {
        case LinphoneCallEnd:
        case LinphoneCallError:
        case LinphoneCallIncoming:
        case LinphoneCallOutgoing:
            [self hidePad:TRUE animated:TRUE];
            [self hideRoutes:TRUE animated:TRUE];
            break;
        default:
            break;
    }
}

- (void)toggleControls:(id)sender {
    bool controlsHidden = (_bottomBar.alpha == 0.0);
    [self hideControls:!controlsHidden sender:sender];
}

- (void)timerHideControls:(id)sender {
    [self hideControls:TRUE sender:sender];
}

- (void)hideControls:(BOOL)hidden sender:(id)sender {
    if (videoHidden && hidden)
        return;

    if (hideControlsTimer) {
        [hideControlsTimer invalidate];
        hideControlsTimer = nil;
    }

    //TODO
    /*if ([[PhoneMainView.instance currentView] equal:CallView.compositeViewDescription]) {
        // show controls
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.35];
        _pausedCallsTable.tableView.alpha = _videoCameraSwitch.alpha = _callPauseButton.alpha = _routesView.alpha =
            _optionsView.alpha = _numpadView.alpha = _bottomBar.alpha = (hidden ? 0 : 1);
        _infoView.alpha = (hidden ? 0 : .8f);

        [UIView commitAnimations];

        [PhoneMainView.instance hideTabBar:hidden];
        [PhoneMainView.instance hideStatusBar:hidden];

        if (!hidden) {
            // hide controls in 5 sec
            hideControlsTimer = [NSTimer scheduledTimerWithTimeInterval:5.0
                                                                 target:self
                                                               selector:@selector(timerHideControls:)
                                                               userInfo:nil
                                                                repeats:NO];
        }
    }*/
}

- (void)disableVideoDisplay:(BOOL)disabled animated:(BOOL)animation {
    if (disabled == videoHidden && animation)
        return;
    videoHidden = disabled;

    if (animation) {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:1.0];
    }

    [_videoGroup setAlpha:disabled ? 0 : 1];

    [self hideControls:!disabled sender:nil];

    if (animation) {
        [UIView commitAnimations];
    }

    // only show camera switch button if we have more than 1 camera
    _videoCameraSwitch.hidden = (disabled || ![[CallManager instance] getFrontCamId]);
    _videoPreview.hidden = (disabled || !linphone_core_self_view_enabled([[CallManager instance] getCore]));

    if (hideControlsTimer != nil) {
        [hideControlsTimer invalidate];
        hideControlsTimer = nil;
    }

    //TODO
    /*if(![PhoneMainView.instance isIphoneXDevice]){
        [PhoneMainView.instance fullScreen:!disabled];
    }
    [PhoneMainView.instance hideTabBar:!disabled];*/

    if (!disabled) {
#ifdef TEST_VIDEO_VIEW_CHANGE
        [NSTimer scheduledTimerWithTimeInterval:5.0
                                         target:self
                                       selector:@selector(_debugChangeVideoView)
                                       userInfo:nil
                                        repeats:YES];
#endif
        // [self batteryLevelChanged:nil];

        [_videoWaitingForFirstImage setHidden:NO];
        [_videoWaitingForFirstImage startAnimating];

        LinphoneCall *call = linphone_core_get_current_call([[CallManager instance] getCore]);
        // linphone_call_params_get_used_video_codec return 0 if no video stream enabled
        if (call != NULL && linphone_call_params_get_used_video_codec(linphone_call_get_current_params(call))) {
            linphone_call_set_next_video_frame_decoded_callback(call, hideSpinner, (__bridge void *)(self));
        }
    }
}

- (void)displayVideoCall:(BOOL)animated {
    [self disableVideoDisplay:FALSE animated:animated];
}

- (void)displayAudioCall:(BOOL)animated {
    [self disableVideoDisplay:TRUE animated:animated];
}

- (void)callDurationUpdate {
    int duration =
        linphone_core_get_current_call([[CallManager instance] getCore]) ? linphone_call_get_duration(linphone_core_get_current_call([[CallManager instance] getCore])) : 0;
    
    NSMutableString *result = [[NSMutableString alloc] init];
    if (duration / 3600 > 0) {
        [result appendString:[NSString stringWithFormat:@"%02i:", duration / 3600]];
        duration = duration % 3600;
    }
    _durationLabel.text = [result stringByAppendingString:[NSString stringWithFormat:@"%02i:%02i", (duration / 60), (duration % 60)]];

    //[_pausedCallsTable update];
    //[_conferenceCallsTable update];
}

- (void)onCurrentCallChange {
    LinphoneCall *call = linphone_core_get_current_call([[CallManager instance] getCore]);

    _noActiveCallView.hidden = (call || linphone_core_is_in_conference([[CallManager instance] getCore]));
    _callView.hidden = !call;
    _conferenceView.hidden = !linphone_core_is_in_conference([[CallManager instance] getCore]);
    _callPauseButton.hidden = NO;// !call && !linphone_core_is_in_conference([[CallManager instance] getCore]);

    [_callPauseButton setType:UIPauseButtonType_CurrentCall call:call];
    [_conferencePauseButton setType:UIPauseButtonType_Conference call:call];

    if (!_callView.hidden) {
        const LinphoneAddress *addr = linphone_call_get_remote_address(call);
        //[ContactDisplay setDisplayNameLabel:_nameLabel forAddress:addr];
        char *uri = linphone_address_as_string_uri_only(addr);
        ms_free(uri);
        //[_avatarImage setImage:[FastAddressBook imageForAddress:addr] bordered:YES withRoundedRadius:YES];
    }
}

- (void)hidePad:(BOOL)hidden animated:(BOOL)animated {
    if (hidden) {
        [_numpadButton setOff];
    } else {
        [_numpadButton setOn];
    }
    if (hidden != _numpadView.hidden) {
        if (animated) {
            [self hideAnimation:hidden forView:_numpadView completion:nil];
        } else {
            [_numpadView setHidden:hidden];
        }
    }
}

- (void)hideRoutes:(BOOL)hidden animated:(BOOL)animated {
    if (hidden) {
        [_routesButton setOff];
    } else {
        [_routesButton setOn];
    }

    _routesBluetoothButton.selected = [CallManager.instance isBluetoothEnabled];
    _routesSpeakerButton.selected = [CallManager.instance isSpeakerEnabled];
    _routesEarpieceButton.selected = !_routesBluetoothButton.selected && !_routesSpeakerButton.selected;

    if (hidden != _routesView.hidden) {
        if (animated) {
            [self hideAnimation:hidden forView:_routesView completion:nil];
        } else {
            [_routesView setHidden:hidden];
        }
    }
}

- (void)hideSpeaker:(BOOL)hidden {
    _speakerButton.hidden = hidden;
    _routesButton.hidden = !hidden;
}

#pragma mark - Event Functions

- (void)bluetoothAvailabilityUpdateEvent:(NSNotification *)notif {
    bool available = [[notif.userInfo objectForKey:@"available"] intValue];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self hideSpeaker:available];
    });
}

- (void)callUpdateEvent:(NSNotification *)notif {
    LinphoneCall *call = [[notif.userInfo objectForKey:@"call"] pointerValue];
    LinphoneCallState state = [[notif.userInfo objectForKey:@"state"] intValue];
    [self callUpdate:call state:state animated:TRUE];
}

- (void)callUpdate:(LinphoneCall *)call state:(LinphoneCallState)state animated:(BOOL)animated {
    [self updateBottomBar:call state:state];
    if (hiddenVolume) {
        //TODO
        //[PhoneMainView.instance setVolumeHidden:FALSE];
        hiddenVolume = FALSE;
    }

    // Update tables
    //[_pausedCallsTable update];
    //[_conferenceCallsTable update];

    static LinphoneCall *currentCall = NULL;
    if (!currentCall || linphone_core_get_current_call([[CallManager instance] getCore]) != currentCall) {
        currentCall = linphone_core_get_current_call([[CallManager instance] getCore]);
        [self onCurrentCallChange];
    }

    // Fake call update
    if (call == NULL) {
        return;
    }

    BOOL shouldDisableVideo = !currentCall || !linphone_call_params_video_enabled(linphone_call_get_current_params(currentCall));
    if (videoHidden != shouldDisableVideo) {
        if (!shouldDisableVideo) {
            [self displayVideoCall:animated];
        } else {
            [self displayAudioCall:animated];
        }
    }
    
    if (!shouldDisableVideo && !linphone_core_is_in_conference([[CallManager instance] getCore]) && // camera is diabled duiring conference, it must be activated after leaving conference.
        [UIApplication sharedApplication].applicationState == UIApplicationStateActive) { // Camera should not be enabled when in background)
        linphone_call_enable_camera(call, TRUE);
    }
    [self updateCallView];

    if (state != LinphoneCallPausedByRemote) {
        _pausedByRemoteView.hidden = YES;
    }

    switch (state) {
        case LinphoneCallIncomingReceived:
        case LinphoneCallOutgoingInit:
        case LinphoneCallConnected:
        case LinphoneCallStreamsRunning: {
            // check video, because video can be disabled because of the low bandwidth.
            if (!linphone_call_params_video_enabled(linphone_call_get_current_params(call))) {
                const LinphoneCallParams *param = linphone_call_get_current_params(call);
                CallAppData *data = [CallManager getAppDataWithCall:call];
                if (state == LinphoneCallStreamsRunning && data && data.videoRequested && linphone_call_params_low_bandwidth_enabled(param)) {
                    // too bad video was not enabled because low bandwidth
                    UIAlertController *errView = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Low bandwidth", nil)
                                                                                     message:NSLocalizedString(@"Video cannot be activated because of low bandwidth "
                                                                                                               @"condition, only audio is available",
                                                                                                               nil)
                                                                              preferredStyle:UIAlertControllerStyleAlert];
                        
                    UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"Continue", nil)
                                                                            style:UIAlertActionStyleDefault
                                                                          handler:^(UIAlertAction * action) {}];
                        
                    [errView addAction:defaultAction];
                    [self presentViewController:errView animated:YES completion:nil];
                    data.videoRequested = FALSE;
                    [CallManager setAppDataWithCall:call appData:data];
                }
            }
            break;
        }
        case LinphoneCallUpdatedByRemote: {
            const LinphoneCallParams *current = linphone_call_get_current_params(call);
            const LinphoneCallParams *remote = linphone_call_get_remote_params(call);

            /* remote wants to add video */
            if ((linphone_core_video_display_enabled([[CallManager instance] getCore]) && !linphone_call_params_video_enabled(current) &&
                 linphone_call_params_video_enabled(remote)) &&
                (!linphone_core_get_video_policy([[CallManager instance] getCore])->automatically_accept ||
                 (([UIApplication sharedApplication].applicationState != UIApplicationStateActive) &&
                  floor(NSFoundationVersionNumber) > NSFoundationVersionNumber_iOS_9_x_Max))) {
                linphone_core_defer_call_update([[CallManager instance] getCore], call);
                [CallManager.instance acceptVideoWithCall:call confirm:TRUE];
            } else if (linphone_call_params_video_enabled(current) && !linphone_call_params_video_enabled(remote)) {
                [self displayAudioCall:animated];
            }
            break;
        }
        case LinphoneCallPausing:
        case LinphoneCallPaused:
            [self displayAudioCall:animated];
            break;
        case LinphoneCallPausedByRemote:
            [self displayAudioCall:animated];
            if (call == linphone_core_get_current_call([[CallManager instance] getCore])) {
                _pausedByRemoteView.hidden = NO;
                [self updateInfoView:TRUE];
            }
            break;
        case LinphoneCallEnd:
        case LinphoneCallError:
        default:
            break;
    }
}

#pragma mark VideoPreviewMoving

- (void)moveVideoPreview:(UIPanGestureRecognizer *)dragndrop {
    CGPoint center = [dragndrop locationInView:_videoPreview.superview];
    _videoPreview.center = center;
    if (dragndrop.state == UIGestureRecognizerStateEnded) {
        [self previewTouchLift];
    }
}

- (CGFloat)coerce:(CGFloat)value betweenMin:(CGFloat)min andMax:(CGFloat)max {
    return MAX(min, MIN(value, max));
}

- (void)previewTouchLift {
    CGRect previewFrame = _videoPreview.frame;
    previewFrame.origin.x = [self coerce:previewFrame.origin.x
                              betweenMin:5
                                  andMax:(UIScreen.mainScreen.bounds.size.width - 5 - previewFrame.size.width)];
    previewFrame.origin.y = [self coerce:previewFrame.origin.y
                              betweenMin:5
                                  andMax:(UIScreen.mainScreen.bounds.size.height - 5 - previewFrame.size.height)];

    if (!CGRectEqualToRect(previewFrame, _videoPreview.frame)) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.3 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
          [UIView animateWithDuration:0.3
                           animations:^{
              //LOGD(@"Recentering preview to %@", NSStringFromCGRect(previewFrame));
                             _videoPreview.frame = previewFrame;
                           }];
        });
    }
}

#pragma mark - Action Functions

- (IBAction)onNumpadClick:(id)sender {
    if ([_numpadView isHidden]) {
        [self hidePad:FALSE animated:YES];
    } else {
        [self hidePad:TRUE animated:YES];
    }
}

- (IBAction)onRoutesBluetoothClick:(id)sender {
    [self hideRoutes:TRUE animated:TRUE];
    [CallManager.instance changeRouteToBluetooth];
}

- (IBAction)onRoutesEarpieceClick:(id)sender {
    [self hideRoutes:TRUE animated:TRUE];
    [CallManager.instance changeRouteToDefault];
}

- (IBAction)onRoutesSpeakerClick:(id)sender {
    [self hideRoutes:TRUE animated:TRUE];
    [CallManager.instance changeRouteToSpeaker];
}

- (IBAction)onRoutesClick:(id)sender {
    if ([_routesView isHidden]) {
        [self hideRoutes:FALSE animated:YES];
    } else {
        [self hideRoutes:TRUE animated:YES];
    }
}

-(void) disconnectCall{
    [[CallManager instance] terminateCallWithCall:linphone_core_get_current_call([[CallManager instance] getCore])];
    [self dismissViewControllerAnimated:true completion:nil];
}

-(void) showNotif:(NSString*)message{

    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

    [self presentViewController:alert animated:YES completion:nil];

    int duration = 5; // duration in seconds

    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, duration * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [alert dismissViewControllerAnimated:YES completion:nil];
    });
}

- (IBAction)onCustomButton1Click:(id)sender {
    NSError *error;
    NSData *objectData = [[self restConfiguration] dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:objectData options:NSJSONReadingMutableContainers error:&error];
    
    NSNumber *cooldownTime  = [json objectForKey:@"cooldownTime"];
    [[self customButton1] setUserInteractionEnabled:NO];
    [NSTimer scheduledTimerWithTimeInterval:cooldownTime.doubleValue target:self selector:@selector(enableCustomButton1) userInfo:nil repeats:NO];
    
    NSNumber *disconnectType  = [json objectForKey:@"disconnectType"];
    
    NSURL *url = [NSURL URLWithString:[json objectForKey:@"url"]];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:url];
    
    NSArray *headers  = [json objectForKey:@"headers"];
    for (int i = 0; i < [headers count]; i++) {
        NSDictionary *header = headers[i];
        NSString *value = [header valueForKey:@"value"];
        NSString *key = [header valueForKey:@"key"];
        [request setValue:value forHTTPHeaderField:key];
    }
    
    [request setHTTPMethod:[json objectForKey:@"method"]];
    if ([[json objectForKey:@"method"]  isEqual: @"POST"]) {
        [request setHTTPBody:[[json objectForKey:@"body"] dataUsingEncoding:NSUTF8StringEncoding]];
        [request setValue:[json objectForKey:@"contentType"] forHTTPHeaderField:@"Content-Type"];
    }
    NSURLSession *session = [NSURLSession sharedSession];
    NSURLSessionTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        NSHTTPURLResponse *rresponse = (NSHTTPURLResponse*)response;
        NSNumber *disconnectOnActionResultDelay  = [json objectForKey:@"DisconnectOnActionResultDelay"];
        if ([rresponse statusCode] == 200) {
            NSNumber* successMessageType = [json objectForKey:@"successMessageType"];
            if (successMessageType.intValue == 0) {
                printf("%s", [[json objectForKey:@"successMessageSpec"] UTF8String]);
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self showNotif:[json objectForKey:@"successMessageSpec"]];
                });
            }else{
                NSString *successMessageSpec = [json objectForKey:@"successMessageSpec"];
                NSArray *successMessageFullPath = [successMessageSpec componentsSeparatedByString:@"/"];
                NSMutableArray *parentArray;
                NSMutableDictionary *parentObject = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];
                int lastParent = 0;
                for (NSString* successMessagePath in successMessageFullPath) {
                    NSArray *elements = [successMessagePath componentsSeparatedByString:@"("];
                    if (elements.count <= 0) {
                        return;
                    }
                    NSString *path = elements[0];
                    NSString *type = [elements[1] componentsSeparatedByString:@")"][0];
                    if([type  isEqual: @"Int"]){
                        if(lastParent ==0){
                            NSInteger result = [[parentObject objectForKey:path] intValue];
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }else{
                            NSInteger result = [[parentArray objectAtIndex:[path intValue]] intValue];
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }
                    }else if([type  isEqual: @"Bool"]){
                        if(lastParent ==0){
                            BOOL result = [parentObject objectForKey:path];
                            printf("%d", result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%d", result]];
                            });
                        }else{
                            BOOL result = [parentArray objectAtIndex:[path intValue]];
                            printf("%d", result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%d", result]];
                            });
                        }
                    }else if([type  isEqual: @"String"]){
                        if(lastParent ==0){
                            NSString* result = [parentObject objectForKey:path];
                            printf("%s", [result UTF8String]);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:result];
                            });
                        }else{
                            NSString* result = [parentArray objectAtIndex:[path intValue]];
                            printf("%s", [result UTF8String]);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:result];
                            });
                        }
                    }else if([type  isEqual: @"JsonObject"]){
                        if(lastParent ==0){
                            parentObject = [parentObject objectForKey:path];
                            
                        }else{
                            parentObject = [parentArray objectAtIndex:[path intValue]];
                        }
                        lastParent = 0;
                    }else if([type  isEqual: @"JsonArray"]){
                        if(lastParent ==0){
                            parentArray = [parentObject objectForKey:path];
                        }else{
                            parentArray = [parentArray objectAtIndex:[path intValue]];
                        }
                        lastParent = 1;
                    }else if([type  isEqual: @"Long"]){
                        if(lastParent ==0){
                            NSNumber *result = @([[parentObject objectForKey:path] longValue]);
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }else{
                            NSNumber *result = @([[parentArray objectAtIndex:[path intValue]] longValue]);
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }
                    }
                }
            }
            if(([disconnectType intValue] == 1 || [disconnectType intValue] == 3) && disconnectOnActionResultDelay.intValue != 0){
                dispatch_async(dispatch_get_main_queue(), ^{
                    [NSTimer scheduledTimerWithTimeInterval:disconnectOnActionResultDelay.doubleValue target:self selector:@selector(disconnectCall) userInfo:nil repeats:NO];
                });
            }
        }else{
            NSNumber* failMessageType = [json objectForKey:@"failMessageType"];
            if (failMessageType.intValue == 0) {
                printf("%s", [[json objectForKey:@"failMessageSpec"] UTF8String]);
                
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self showNotif:[json objectForKey:@"failMessageSpec"]];
                });
            }else{
                NSString *failMessageSpec = [json objectForKey:@"failMessageSpec"];
                NSArray *failMessageFullPath = [failMessageSpec componentsSeparatedByString:@"/"];
                NSMutableArray *parentArray;
                NSMutableDictionary *parentObject = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];;
                int lastParent = 0;
                for (NSString* failMessagePath in failMessageFullPath) {
                    NSArray *elements = [failMessagePath componentsSeparatedByString:@"("];
                    if (elements.count <= 0) {
                        return;
                    }
                    NSString *path = elements[0];
                    NSString *type = [elements[1] componentsSeparatedByString:@")"][0];
                    if([type  isEqual: @"Int"]){
                        if(lastParent ==0){
                            NSInteger result = [[parentObject objectForKey:path] intValue];
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }else{
                            NSInteger result = [[parentArray objectAtIndex:[path intValue]] intValue];
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }
                    }else if([type  isEqual: @"Bool"]){
                        if(lastParent ==0){
                            BOOL result = [parentObject objectForKey:path];
                            printf("%d", result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%d", result]];
                            });
                        }else{
                            BOOL result = [parentArray objectAtIndex:[path intValue]];
                            printf("%d", result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%d", result]];
                            });
                        }
                    }else if([type  isEqual: @"String"]){
                        if(lastParent ==0){
                            NSString* result = [parentObject objectForKey:path];
                            printf("%s", [result UTF8String]);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:result];
                            });
                        }else{
                            NSString* result = [parentArray objectAtIndex:[path intValue]];
                            printf("%s", [result UTF8String]);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:result];
                            });
                        }
                    }else if([type  isEqual: @"JsonObject"]){
                        if(lastParent ==0){
                            parentObject = [parentObject objectForKey:path];
                            
                        }else{
                            parentObject = [parentArray objectAtIndex:[path intValue]];
                        }
                        lastParent = 0;
                    }else if([type  isEqual: @"JsonArray"]){
                        if(lastParent ==0){
                            parentArray = [parentObject objectForKey:path];
                        }else{
                            parentArray = [parentArray objectAtIndex:[path intValue]];
                        }
                        lastParent = 1;
                    }else if([type  isEqual: @"Long"]){
                        if(lastParent ==0){
                            NSNumber *result = @([[parentObject objectForKey:path] longValue]);
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }else{
                            NSNumber *result = @([[parentArray objectAtIndex:[path intValue]] longValue]);
                            printf("%ld", (long)result);
                            dispatch_async(dispatch_get_main_queue(), ^{
                                [self showNotif:[NSString stringWithFormat:@"%ld", (long)result]];
                            });
                        }
                    }
                }
            }
            if(([disconnectType intValue] == 2 || [disconnectType intValue] == 3) && disconnectOnActionResultDelay.intValue != 0){
                dispatch_async(dispatch_get_main_queue(), ^{
                    [NSTimer scheduledTimerWithTimeInterval:disconnectOnActionResultDelay.doubleValue target:self selector:@selector(disconnectCall) userInfo:nil repeats:NO];
                });
            }
        }
            
    }];
    [task resume];
}

-(void)enableCustomButton1{
    [[self customButton1] setUserInteractionEnabled:YES];
    
}
-(void)enableCustomButton2{
    
    [[self customButton2] setUserInteractionEnabled:YES];
}


- (IBAction)onCustomButton2Click:(id)sender {
    NSError *error;
    NSData *objectData = [[self dmftConfiguration] dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:objectData options:NSJSONReadingMutableContainers error:&error];
    NSString *sequence = [json objectForKey:@"sequence"];
    NSNumber *cooldownTime  = [json objectForKey:@"cooldownTime"];
    [[self customButton2] setUserInteractionEnabled:NO];
    [NSTimer scheduledTimerWithTimeInterval:cooldownTime.doubleValue target:self selector:@selector(enableCustomButton2) userInfo:nil repeats:NO];
    int result = 0;
    NSNumber *disconnectType  = [json objectForKey:@"disconnectType"];
    for (int i = 0; i < [sequence length]; i++) {
        char key = [sequence characterAtIndex:i];
        if (key == ',') {
            [NSThread sleepForTimeInterval:1.0f];
            continue;
        }
        result = result + linphone_call_send_dtmf(linphone_core_get_current_call([[CallManager instance] getCore]), key);
        linphone_core_play_dtmf([[CallManager instance] getCore], key, 100);
        

    }
    NSNumber *disconnectOnActionResultDelay  = [json objectForKey:@"DisconnectOnActionResultDelay"];
    if (result == 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self showNotif:[json objectForKey:@"successMessage"]];
        });
        if(([disconnectType intValue] == 1 || [disconnectType intValue] == 3) && disconnectOnActionResultDelay.intValue != 0){
            dispatch_async(dispatch_get_main_queue(), ^{
                [NSTimer scheduledTimerWithTimeInterval:disconnectOnActionResultDelay.doubleValue target:self selector:@selector(disconnectCall) userInfo:nil repeats:NO];
            });
        }
    }else{
        dispatch_async(dispatch_get_main_queue(), ^{
            [self showNotif:[json objectForKey:@"failMessage"]];
        });
        if(([disconnectType intValue] == 2 || [disconnectType intValue] == 3) && disconnectOnActionResultDelay.intValue != 0){
            dispatch_async(dispatch_get_main_queue(), ^{
                [NSTimer scheduledTimerWithTimeInterval:disconnectOnActionResultDelay.doubleValue target:self selector:@selector(disconnectCall) userInfo:nil repeats:NO];
            });
        }
    }
    //Log.e(Linphone.TAG,input.getString("failMessage"));
    //Log.i(Linphone.TAG,input.getString("successMessage"));
}
/*
- (IBAction)onOptionsTransferClick:(id)sender {
    [self hideOptions:TRUE animated:TRUE];
    DialerView *view = VIEW(DialerView);
    [view setAddress:@""];
    CallManager.instance.nextCallIsTransfer = TRUE;
    [PhoneMainView.instance changeCurrentView:view.compositeViewDescription];
}

- (IBAction)onOptionsAddClick:(id)sender {
    [self hideOptions:TRUE animated:TRUE];
    DialerView *view = VIEW(DialerView);
    [view setAddress:@""];
    CallManager.instance.nextCallIsTransfer = FALSE;
    [PhoneMainView.instance changeCurrentView:view.compositeViewDescription];
}

- (IBAction)onOptionsConferenceClick:(id)sender {
    [self hideOptions:TRUE animated:TRUE];
    [CallManager.instance groupCall];
}*/

#pragma mark - Animation

- (void)hideAnimation:(BOOL)hidden forView:(UIView *)target completion:(void (^)(BOOL finished))completion {
    if (hidden) {
    int original_y = target.frame.origin.y;
    CGRect newFrame = target.frame;
    newFrame.origin.y = self.view.frame.size.height;
    [UIView animateWithDuration:0.5
        delay:0.0
        options:UIViewAnimationOptionCurveEaseIn
        animations:^{
          target.frame = newFrame;
        }
        completion:^(BOOL finished) {
          CGRect originFrame = target.frame;
          originFrame.origin.y = original_y;
          target.hidden = YES;
          target.frame = originFrame;
          if (completion)
              completion(finished);
        }];
    } else {
        CGRect frame = target.frame;
        int original_y = frame.origin.y;
        frame.origin.y = self.view.frame.size.height;
        target.frame = frame;
        frame.origin.y = original_y;
        target.hidden = NO;

        [UIView animateWithDuration:0.5
            delay:0.0
            options:UIViewAnimationOptionCurveEaseOut
            animations:^{
              target.frame = frame;
            }
            completion:^(BOOL finished) {
              target.frame = frame; // in case application did not finish
              if (completion)
                  completion(finished);
            }];
    }
}
@end
