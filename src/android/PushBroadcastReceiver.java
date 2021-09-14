/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
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
package com.outsystems.linphone.pushnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.outsystems.linphone.CallActivity;
import com.outsystems.linphone.Linphone;

import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;

public class PushBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        // A push have been received but there was no Core alive, you should create it again
        // This way the core will register and it will handle the message or call event like if the app was started

        if (intent.getAction().equals("org.linphone.HANGUP_CALL_ACTION") || intent.getAction().equals("org.linphone.ANSWER_CALL_ACTION")) {
            handleCallIntent(context,intent);
        }else{
            Linphone.callbackOS("PushNotifications","Received",intent.getAction());
        }
    }

    private void handleCallIntent(Context context,Intent intent) {
        String remoteSipAddress = intent.getStringExtra("REMOTE_ADDRESS");
        if (remoteSipAddress == null) {
            Log.e("[Notification Broadcast Receiver] Remote SIP address is null for notification");
            return;
        }

        if (intent.getAction().equals("org.linphone.ANSWER_CALL_ACTION")) {
            Intent callActivity = new Intent(context,CallActivity.class);
            callActivity.putExtra("remoteSipAddress",remoteSipAddress);
            context.startActivity(callActivity);
        } else {
            Address remoteAdress = Linphone.core.interpretUrl(remoteSipAddress);

            Call call = (remoteAdress != null) ? Linphone.core.getCallByRemoteAddress2(remoteAdress) : null;

            if (call == null) {
                Log.e("[Notification Broadcast Receiver] Couldn't find call from remote address $remoteSipAddress");
                return;
            }
            if (call.getState() == Call.State.IncomingReceived || call.getState() == Call.State.IncomingEarlyMedia){
                call.decline(Reason.Declined);
            } else{
                call.terminate();
            }
        }
    }
}