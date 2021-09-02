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
package com.outsystems.linphone;

import android.Manifest;
import android.content.Intent;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.linphone.core.*;

public class Linphone extends CordovaPlugin {
    public static final String TAG = "Hello Linphone Plugin";

    public static Core core;
    public static String DTMFToneInput;
    public static String RESTInput;
    private CoreListenerStub coreListener;
    private static CallbackContext listenerCB;
    private CallbackContext callback;
    private static String proxyServer;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // For push notifications to work, you have to copy your google-services.json in the app/ folder
        // And you must declare our FirebaseMessaging service in the Manifest
        // You also have to make some changes in your build.gradle files, see the ones in this project

        Factory factory = Factory.instance();
        factory.setDebugMode(true, "Hello Linphone");
        core = factory.createCore(null, null, cordova.getActivity());

        // Make sure the core is configured to use push notification token from firebase
        core.setPushNotificationEnabled(true);


        // For video to work, we need two TextureViews:
        // one for the remote video and one for the local preview
        //core.setNativeVideoWindowId(cordova.getActivity().findViewById(R.id.remote_video_surface));
        // The local preview is a org.linphone.mediastream.video.capture.CaptureTextureView
        // which inherits from TextureView and contains code to keep the ratio of the capture video
        //core.setNativePreviewWindowId(cordova.getActivity().findViewById(R.id.local_preview_video_surface));

        // Here we enable the video capture & display at Core level
        // It doesn't mean calls will be made with video automatically,
        // But it allows to use it later
        core.enableVideoCapture(true);
        core.enableVideoDisplay(true);

        // When enabling the video, the remote will either automatically answer the update request
        // or it will ask it's user depending on it's policy.
        // Here we have configured the policy to always automatically accept video requests
        core.getVideoActivationPolicy().setAutomaticallyAccept(true);
        // If you don't want to automatically accept,
        // you'll have to use a code similar to the one in toggleVideo to answer a received request

        // If the following property is enabled, it will automatically configure created call params with video enabled
        //core.videoActivationPolicy.automaticallyInitiate = true

    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        switch (action){
            case "setListeners":
                listenerCB = callbackContext;
                setListeners();
                return true;
            case "connect":
                login(args.getString(0), args.getString(1),args.getString(2),args.getString(3),args.getString(4),args.getString(5),args.getString(6),args.getString(7),args.getString(8));
                return true;
            case "disconnect":
                unregister();
                return true;
            case "delete":
                delete();
                return true;
            case "call":
                if (args.getBoolean(1) && !cordova.hasPermission(Manifest.permission.CAMERA)){
                    cordova.requestPermission(this,1,Manifest.permission.CAMERA);
                }
                Intent call = new Intent(cordova.getActivity(),CallActivity.class);
                call.putExtra("Type","Call");
                call.putExtra("Domain",args.getString(0));
                call.putExtra("Video",args.getBoolean(1));
                call.putExtra("LowBandwidth",args.getBoolean(2));
                cordova.getActivity().startActivity(call);
                return true;
            case "receiveCall":
                if (!cordova.hasPermission(Manifest.permission.CAMERA)){
                    cordova.requestPermission(this,1,Manifest.permission.CAMERA);
                }
                Intent ringing = new Intent(cordova.getActivity(),CallActivity.class);
                ringing.putExtra("Video",true);
                ringing.putExtra("Type","Ringing");
                cordova.getActivity().startActivity(ringing);
                return true;
            case "togglePause":
                pauseOrResume();
                return true;
            case "hangup":
                // Terminates the call, whether it is ringing or running
                hangUp();
                return true;
            case "toggleMic":
                // The following toggles the microphone, disabling completely / enabling the sound capture
                // from the device microphone
                core.enableMic(!core.micEnabled());
                return true;
            case "toggleSpeaker":
                toggleSpeaker();
                return true;
            case "toggleVideo":
                toggleVideo();
                return true;
            case "toggleCamera":
                toggleCamera();
                return true;
            case "setCustomButton1":
                RESTInput = args.getString(0);
                return true;
            case "setCustomButton2":
                DTMFToneInput = args.getString(0);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                Linphone.core.reloadVideoDevices();
                break;
            case 0:
                Linphone.core.reloadSoundDevices();
                break;
            default:
                break;
        }
    }

    public void setListeners(){
        Log.d(TAG,"Added Listeners!");
        coreListener = new CoreListenerStub(){
            @Override
            public void onAccountRegistrationStateChanged(@NonNull Core core, @NonNull Account account, RegistrationState state, @NonNull String message) {
                super.onAccountRegistrationStateChanged(core, account, state, message);
                //findViewById<TextView>(R.id.registration_status).text = message
                Log.d(TAG,"Account Registration State Changed!");
                JSONObject objReply;
                PluginResult result = null;
                if (state == RegistrationState.Failed) {
                    try {
                        objReply = new JSONObject("{\"Type\":\"Registration\",\"Status\":\"Failed\",\"Message\":\""+message+"\"}");

                        result = new PluginResult(PluginResult.Status.OK,objReply);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                    }
                } else if (state == RegistrationState.Ok) {
                    try {
                        objReply = new JSONObject("{\"Type\":\"Registration\",\"Status\":\"Success\",\"Message\":\""+account.getParams().getContactUriParameters()+"\"}");
                        result = new PluginResult(PluginResult.Status.OK,objReply);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                    }


                    // This will display the push information stored in the contact URI parameters
                    //findViewById<TextView>(R.id.push_info).text = account.params.contactUriParameters
                }else{
                    try {
                        objReply = new JSONObject("{\"Type\":\"Registration\",\"Status\":\"Unknown\"}");
                        result = new PluginResult(PluginResult.Status.OK,objReply);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                    }
                }

                result.setKeepCallback(true);
                if (listenerCB != null){
                    listenerCB.sendPluginResult(result);
                }
            }

            @Override
            public void onAudioDeviceChanged(@NonNull Core core, @NonNull AudioDevice audioDevice) {
                // This callback will be triggered when a successful audio device has been changed
                Log.d(TAG,"Audio Device Changed!");
                PluginResult result;
                try {
                    JSONObject objReply = new JSONObject("{\"Type\":\"AudioChanged\"}");
                    result = new PluginResult(PluginResult.Status.OK,objReply);
                } catch (JSONException e) {
                    e.printStackTrace();
                    result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                }
                result.setKeepCallback(true);
                if (listenerCB != null){
                    listenerCB.sendPluginResult(result);
                }

                super.onAudioDeviceChanged(core, audioDevice);
            }

            @Override
            public void onAudioDevicesListUpdated(@NonNull Core core) {
                // This callback will be triggered when the available devices list has changed,
                // for example after a bluetooth headset has been connected/disconnected.
                Log.d(TAG,"Audio devices list updated!");
                PluginResult result;
                try {
                    JSONObject objReply = new JSONObject("{\"Type\":\"AudioUpdated\"}");
                    result = new PluginResult(PluginResult.Status.OK,objReply);
                } catch (JSONException e) {
                    e.printStackTrace();
                    result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                }
                result.setKeepCallback(true);
                if (listenerCB != null){
                    listenerCB.sendPluginResult(result);
                }

                super.onAudioDevicesListUpdated(core);
            }

            @Override
            public void onCallStateChanged(@NonNull Core core, @NonNull Call call, Call.State state, @NonNull String message) {
                super.onCallStateChanged(core, call, state, message);
                //findViewById<TextView>(R.id.call_status).text = message
                Log.d(TAG,"Call State Changed!");
                JSONObject objReply;
                PluginResult result = null;
                // When a call is received
                switch (state) {
                    case IncomingReceived :
                        Log.d(TAG,"IncomingReceived");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"IncomingCall\",\"Caller\":\""+call.getRemoteAddress().asStringUriOnly()+"\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        Intent answer = new Intent(cordova.getActivity(),CallActivity.class);
                        answer.putExtra("Video",false);
                        answer.putExtra("Type","Ringing");
                        cordova.getActivity().startActivity(answer);
                        //findViewById<EditText>(R.id.remote_address).setText(call.remoteAddress.asStringUriOnly())
                        break;

                    case Connected :
                        Log.d(TAG,"Connected");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Connected\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        //findViewById<Button>(R.id.mute_mic).isEnabled = true
                        //findViewById<Button>(R.id.toggle_speaker).isEnabled = true
                        break;

                    case Released :
                        Log.d(TAG,"Released");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Released\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // Call state will be released shortly after the End state
                        break;
                    case OutgoingInit:
                        Log.d(TAG,"OutgoingInit");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"OutgoingInit\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // First state an outgoing call will go through
                        break;
                    case OutgoingProgress:
                        Log.d(TAG,"OutgoingProgress");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"OutgoingProgress\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // Right after outgoing init
                        break;
                    case OutgoingRinging:
                        Log.d(TAG,"OutgoingRinging");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"OutgoingRinging\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // This state will be reached upon reception of the 180 RINGING
                        break;
                    case StreamsRunning:
                        Log.d(TAG,"StreamsRunning");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"StreamsRunning\",\"isVideoEnabled\":\""+(core.getVideoDevicesList().length > 2 && call.getCurrentParams().videoEnabled())+"\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // This state indicates the call is active.
                        // You may reach this state multiple times, for example after a pause/resume
                        // or after the ICE negotiation completes
                        // Wait for the call to be connected before allowing a call update

                        // Only enable toggle camera button if there is more than 1 camera and the video is enabled
                        // We check if core.videoDevicesList.size > 2 because of the fake camera with static image created by our SDK (see below)
                        //findViewById<Button>(R.id.toggle_camera).isEnabled = core.videoDevicesList.size > 2 && call.currentParams.videoEnabled()
                        break;
                    case Paused:
                        Log.d(TAG,"Paused");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Paused\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // When you put a call in pause, it will became Paused
                        break;
                    case PausedByRemote:
                        Log.d(TAG,"PausedByRemote");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"PausedByRemote\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // When the remote end of the call pauses it, it will be PausedByRemote
                        break;
                    case Updating:
                        Log.d(TAG,"Updating");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Updating\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // When we request a call update, for example when toggling video
                        break;
                    case UpdatedByRemote:
                        Log.d(TAG,"UpdatedByRemote");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"UpdatedByRemote\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        // When the remote requests a call update
                        break;
                    case Error:
                        Log.d(TAG,"Error");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Error\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case End:
                        Log.d(TAG,"End");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"End\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case Idle:
                        Log.d(TAG,"Idle");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Idle\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case Pausing:
                        Log.d(TAG,"Pausing");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Pausing\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case Resuming:
                        Log.d(TAG,"Resuming");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Resuming\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case EarlyUpdating:
                        Log.d(TAG,"EarlyUpdating");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"EarlyUpdating\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case Referred:
                        Log.d(TAG,"Referred");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"Referred\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case IncomingEarlyMedia:
                        Log.d(TAG,"IncomingEarlyMedia");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"IncomingEarlyMedia\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case OutgoingEarlyMedia:
                        Log.d(TAG,"OutgoingEarlyMedia");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"OutgoingEarlyMedia\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case EarlyUpdatedByRemote:
                        Log.d(TAG,"EarlyUpdatedByRemote");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"EarlyUpdatedByRemote\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;
                    case PushIncomingReceived:
                        Log.d(TAG,"PushIncomingReceived");
                        try {
                            objReply = new JSONObject("{\"Type\":\"CallState\",\"State\":\"PushIncomingReceived\",\"Message\":\""+message+"\"}");
                            result = new PluginResult(PluginResult.Status.OK,objReply);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            result = new PluginResult(PluginResult.Status.OK,e.getLocalizedMessage());
                        }
                        break;

                }
                result.setKeepCallback(true);
                if (listenerCB != null){
                    listenerCB.sendPluginResult(result);
                }
            }

        };
        core.addListener(coreListener);
    }


    public void login(@NonNull String username,String userid, @NonNull String password,String ha1,String realm, @NonNull String domain,String algorithm,String proxy, String transportType) {
        TransportType type;
        switch (transportType){
            case "UDP":
                type = TransportType.Udp;
                break;
            case "TCP":
                type = TransportType.Tcp;
                break;
            default:
                type = TransportType.Tls;
                break;
        }
        AuthInfo authInfo = Factory.instance().createAuthInfo(username,null,password,null,null,domain,null);

        AccountParams params = core.createAccountParams();
        Address identity = Factory.instance().createAddress("sip:"+username+"@"+domain);
        params.setIdentityAddress(identity);

        proxyServer = proxy;
        Address address = Factory.instance().createAddress("sip:"+proxy+";transport="+transportType.toLowerCase());
        address.setTransport(type);
        params.setServerAddress(address);
        params.setRegisterEnabled(true);

        // Ensure push notification is enabled for this account
        params.setPushNotificationAllowed(true);

        core.addAuthInfo(authInfo);
        Account account = core.createAccount(params);
        core.addAccount(account);

        core.setDefaultAccount(account);
        core.start();

        if (!core.isPushNotificationEnabled()) {
            Toast.makeText(cordova.getActivity(), "Something is wrong with the push setup!", Toast.LENGTH_LONG).show();
        }

        // We will need the RECORD_AUDIO permission for video call
        if (!cordova.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            cordova.requestPermission(this,0,Manifest.permission.RECORD_AUDIO);
        }

        // And that's it!
        // You can kill this app and send a message or initiate a call to the identity you registered and you'll see the toast.

        // When a push notification will be received by your app, either:
        // - the Core is alive and it will check it is properly registered & connected to the proxy
        // - the Core isn't available and a broadcast on org.linphone.core.action.PUSH_RECEIVED will be fired

        // Another way is to create your own Application object and create the Core in it
        // This way, when a push will be received, the Core will be created before the push being handled
        // so the first case above will always be true. See our linphone-android app for an example of that.
    }

    public static void toggleSpeaker() throws NullPointerException{
        // Get the currently used audio device
        Call currentCall = core.getCurrentCall();
        AudioDevice currentAudioDevice = null;
        if (currentCall != null) {
            currentAudioDevice = currentCall.getOutputAudioDevice();
            Boolean speakerEnabled = currentAudioDevice.getType() == AudioDevice.Type.Speaker;

            // We can get a list of all available audio devices using
            // Note that on tablets for example, there may be no Earpiece device
            for (AudioDevice audioDevice : core.getAudioDevices()) {
                if (speakerEnabled && audioDevice.getType() == AudioDevice.Type.Earpiece) {
                    core.getCurrentCall().setOutputAudioDevice(audioDevice);
                    return;
                } else if (!speakerEnabled && audioDevice.getType() == AudioDevice.Type.Speaker) {
                    core.getCurrentCall().setOutputAudioDevice(audioDevice);
                    return;
                }/* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
            }
        }
    }

    private void unregister() {
        // Here we will disable the registration of our Account
        Account account = core.getDefaultAccount();
        if (account == null) {
            return;
        }

        AccountParams params = account.getParams();
        // Returned params object is const, so to make changes we first need to clone it
        AccountParams clonedParams = params.clone();

        // Now let's make our changes
        clonedParams.setRegisterEnabled(false);

        // And apply them
        account.setParams(clonedParams);
    }

    private void delete() {
        // To completely remove an Account
        Account account = core.getDefaultAccount();
        if (account == null) {
            return;
        }
        core.removeAccount(account);

        // To remove all accounts use
        core.clearAccounts();

        // Same for auth info
        core.clearAllAuthInfo();
    }

    public static void outgoingCall(String domain,Boolean video,Boolean lowBandwith) {
        if(!core.isNetworkReachable()){
            Log.e(TAG,"Network unreachable, aborting Call!");
            return;
        }

        // As for everything we need to get the SIP URI of the remote and convert it to an Address
        if(proxyServer != null){
            domain = domain.split("@")[0] +"@"+ proxyServer;
        }
        Address remoteAddress = Factory.instance().createAddress("sip:"+domain);
        // If address parsing fails, we can't continue with outgoing call process
        if (remoteAddress == null) {
            return;
        }
        if(core.getDefaultAccount() != null){
            Address localAddress = core.getDefaultAccount().getContactAddress();
            if(localAddress != null){
                remoteAddress.setTransport(localAddress.getTransport());
            }
        }
        // We also need a CallParams object
        // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
        CallParams params = Linphone.core.createCallParams(null);

        // Same for params
        if (params == null) {
            core.inviteAddress(remoteAddress);
            return;
        }

        if(lowBandwith){
            params.enableLowBandwidth(true);
        }
        // We can now configure it
        // Here we ask for no encryption but we could ask for ZRTP/SRTP/DTLS
        params.setMediaEncryption(MediaEncryption.None);
        // If we wanted to start the call with video directly
        params.enableVideo(video);

        // Finally we start the call
        Linphone.core.inviteAddressWithParams(remoteAddress, params);
        // Call process can be followed in onCallStateChanged callback from core listener
    }

    public static void hangUp() {
        if (core.getCallsNb() == 0){
            return;
        }
        // If the call state isn't paused, we can get it using core.currentCall
        Call curCall = (core.getCurrentCall() != null) ? core.getCurrentCall() : core.getCalls()[0];
        if (curCall != null) {
            // Terminating a call is quite simple
            curCall.terminate();
        }
    }

    public static void toggleVideo(Boolean state) {
        if (core.getCallsNb() == 0){
            return;
        }
        Call curCall = (core.getCurrentCall() != null) ? core.getCurrentCall() : core.getCalls()[0];
        if (curCall == null) {
            return;
        }

        // To update the call, we need to create a new call params, from the call object this time
        CallParams params = core.createCallParams(curCall);
        // Here we toggle the video state (disable it if enabled, enable it if disabled)
        // Note that we are using currentParams and not params or remoteParams
        // params is the object you configured when the call was started
        // remote params is the same but for the remote
        // current params is the real params of the call, resulting of the mix of local & remote params
        params.enableVideo(state);
        if (state){
            core.enableVideoCapture(true);
            core.enableVideoDisplay(true);
        }
        // Finally we request the call update
        curCall.update(params);

        // Note that when toggling off the video, TextureViews will keep showing the latest frame displayed
    }

    public static void toggleVideo() {
        if (core.getCallsNb() == 0){
            return;
        }
        Call curCall = (core.getCurrentCall() != null) ? core.getCurrentCall() : core.getCalls()[0];
        if (curCall == null) {
            return;
        }

        // To update the call, we need to create a new call params, from the call object this time
        CallParams params = core.createCallParams(curCall);
        // Here we toggle the video state (disable it if enabled, enable it if disabled)
        // Note that we are using currentParams and not params or remoteParams
        // params is the object you configured when the call was started
        // remote params is the same but for the remote
        // current params is the real params of the call, resulting of the mix of local & remote params
        params.enableVideo(!curCall.getCurrentParams().videoEnabled());
        if (!curCall.getCurrentParams().videoEnabled()){
            core.enableVideoCapture(true);
            core.enableVideoDisplay(true);
        }
        // Finally we request the call update
        curCall.update(params);

        // Note that when toggling off the video, TextureViews will keep showing the latest frame displayed
    }

    public static void toggleCamera() {
        // Currently used camera
        String currentDevice = core.getVideoDevice();

        // Let's iterate over all camera available and choose another one
        for (String camera : core.getVideoDevicesList()) {
            // All devices will have a "Static picture" fake camera, and we don't want to use it
            if (!camera.equals(currentDevice) && !camera.equals("StaticImage: Static picture")) {
                core.setVideoDevice(camera);
                break;
            }
        }
    }

    public static void pauseOrResume() {
        if (core.getCallsNb() == 0){
            return;
        }
        Call curCall = (core.getCurrentCall() != null) ? core.getCurrentCall() : core.getCalls()[0];
        if (curCall == null) {
            return;
        }

        if (curCall.getState() != Call.State.Paused && curCall.getState() != Call.State.Pausing) {
            // If our call isn't paused, let's pause it
            curCall.pause();
        } else if (curCall.getState() != Call.State.Resuming) {
            // Otherwise let's resume it
            curCall.resume();
        }
    }
}