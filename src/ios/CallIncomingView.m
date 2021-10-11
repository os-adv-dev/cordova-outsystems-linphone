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

#import "CallIncomingView.h"
//#import "FastAddressBook.h"
//#import "PhoneMainView.h"
#import "Utils.h"
#import "$appname-Swift.h"
#import "CallView.h"

@implementation CallIncomingView

#pragma mark - ViewController Functions

- (void)viewWillAppear:(BOOL)animated {
	[super viewWillAppear:animated];

	[NSNotificationCenter.defaultCenter addObserver:self
										   selector:@selector(callUpdateEvent:)
											   name:@"LinphoneCallUpdate"
											 object:nil];
}

- (void)viewWillDisappear:(BOOL)animated {
	[super viewWillDisappear:animated];
	[NSNotificationCenter.defaultCenter removeObserver:self name:@"LinphoneCallUpdate" object:nil];
	_call = NULL;
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
	[super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
	if (_earlyMedia && linphone_core_get_calls_nb([[CallManager instance] getCore]) < 2) {
		_earlyMediaView.hidden = NO;
		linphone_core_set_native_video_window_id([[CallManager instance] getCore], (__bridge void *)(_earlyMediaView));
	}
	if (_call) {
		[self update];
	}
}

#pragma mark - Event Functions

- (void)callUpdateEvent:(NSNotification *)notif {
	LinphoneCall *acall = [[notif.userInfo objectForKey:@"call"] pointerValue];
	LinphoneCallState astate = [[notif.userInfo objectForKey:@"state"] intValue];
	[self callUpdate:acall state:astate];
}

//TODO add auto_answer
- (void)callUpdate:(LinphoneCall *)acall state:(LinphoneCallState)astate {
	if (_call == acall && (astate == LinphoneCallEnd || astate == LinphoneCallError)) {
		[_delegate incomingCallAborted:_call];
	}/* else if ([LinphoneManager.instance lpConfigBoolForKey:@"auto_answer"]) {
		LinphoneCallState state = linphone_call_get_state(_call);
		if (state == LinphoneCallIncomingReceived) {
			NSLog(@"Auto answering call");
			[self onAcceptClick:nil];
		}
	}*/
}

#pragma mark -

- (void)update {
	const LinphoneAddress *addr = linphone_call_get_remote_address(_call);
	//[ContactDisplay setDisplayNameLabel:_nameLabel forAddress:addr withAddressLabel:_addressLabel];
	char *uri = linphone_address_as_string_uri_only(addr);
	ms_free(uri);
	//[_avatarImage setImage:[FastAddressBook imageForAddress:addr] bordered:YES withRoundedRadius:YES];

	_tabBar.hidden = linphone_call_params_video_enabled(linphone_call_get_remote_params(_call));
	_tabVideoBar.hidden = !_tabBar.hidden;
}

#pragma mark - Property Functions
static void hideSpinner(LinphoneCall *call, void *user_data) {
	CallIncomingView *thiz = (__bridge CallIncomingView *)user_data;
	thiz.earlyMedia = TRUE;
	thiz.earlyMediaView.hidden = NO;
    LinphoneCore * core = thiz.core;
	linphone_core_set_native_video_window_id(core, (__bridge void *)(thiz.earlyMediaView));
}

- (void)setCall:(LinphoneCall *)call {
	_call = call;
	_earlyMedia = FALSE;
	if ( linphone_core_get_calls_nb([[CallManager instance] getCore]) < 2) {
		linphone_call_accept_early_media(_call);
		// linphone_call_params_get_used_video_codec return 0 if no video stream enabled
		if (linphone_call_params_get_used_video_codec(linphone_call_get_current_params(_call))) {
			linphone_call_set_next_video_frame_decoded_callback(call, hideSpinner, (__bridge void *)(self));
		}
	} else {
		_earlyMediaView.hidden = YES;
	}
	
	[self update];
	[self callUpdate:_call state:linphone_call_get_state(call)];
}

#pragma mark - Action Functions

- (IBAction)onAcceptClick:(id)event {
	[_delegate incomingCallAccepted:_call evenWithVideo:YES];
}

- (IBAction)onDeclineClick:(id)event {
	[_delegate incomingCallDeclined:_call];
}

- (IBAction)onAcceptAudioOnlyClick:(id)sender {
	[_delegate incomingCallAccepted:_call evenWithVideo:NO];
}

@end
