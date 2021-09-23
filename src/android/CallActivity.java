package com.outsystems.linphone;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import $appid.MainActivity;
import $appid.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import okhttp3.Response;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseListener;

import java.util.Timer;
import java.util.TimerTask;

public class CallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidNetworking.initialize(getApplicationContext());
        setContentView(R.layout.callscreen);
        // For video to work, we need two TextureViews:
        // one for the remote video and one for the local preview
        Linphone.core.setNativeVideoWindowId(findViewById(R.id.remote_video_surface));
        // The local preview is a org.linphone.mediastream.video.capture.CaptureTextureView
        // which inherits from TextureView and contains code to keep the ratio of the capture video
        Linphone.core.setNativePreviewWindowId(findViewById(R.id.local_preview_video_surface));

        Intent currIntent = getIntent();
        String type = (currIntent.getStringExtra("Type") != null) ? currIntent.getStringExtra("Type") : "";
        switch (type){
            case "Call":
                String domain = (currIntent.getStringExtra("Domain") != null) ? currIntent.getStringExtra("Domain") : "";
                if(domain.equals("")){
                    finish();
                    return;
                }
                Boolean outVideo = currIntent.getBooleanExtra("Video",false);
                Boolean lowBandwidth = currIntent.getBooleanExtra("LowBandwidth",false);
                if (outVideo){
                    String[] videoDevices = Linphone.core.getVideoDevicesList();
                    if (videoDevices.length>1){
                        Linphone.core.setVideoDevice(videoDevices[1]);
                    }
                }
                Linphone.outgoingCall(domain,outVideo,lowBandwidth);
                OugoingCall();
                break;
            case "Ringing":
                Boolean inVideo = currIntent.getBooleanExtra("Video",false);
                if (inVideo){
                    String[] videoDevices = Linphone.core.getVideoDevicesList();
                    if (videoDevices.length>1){
                        Linphone.core.setVideoDevice(videoDevices[1]);
                    }
                }
                IncomingCall(inVideo);
                break;
            case "Answer":
                String remoteSipAddress = currIntent.getStringExtra("remoteSipAddress");
                answerCall(remoteSipAddress);

            default:
                finish();
                break;
        }

        Linphone.core.addListener(new CoreListenerStub(){
            @Override
            public void onCallStateChanged(@NonNull Core core, @NonNull Call call, Call.State state, @NonNull String message) {
                super.onCallStateChanged(core, call, state, message);
                switch (state) {
                    case Connected :
                        Log.d(Linphone.TAG,"Connected");
                        Call();
                        break;

                    case Released :
                        Log.d(Linphone.TAG,"Released");
                        Intent mainAct = new Intent(CallActivity.this, MainActivity.class);
                        startActivity(mainAct);
                        // Call state will be released shortly after the End state
                        break;
                    case Paused:
                        Log.d(Linphone.TAG,"Paused");
                        // When you put a call in pause, it will became Paused
                        break;
                    case PausedByRemote:
                        Log.d(Linphone.TAG,"PausedByRemote");
                        // When the remote end of the call pauses it, it will be PausedByRemote
                        break;
                    case Updating:
                        Log.d(Linphone.TAG,"Updating");
                        // When we request a call update, for example when toggling video
                        break;
                    case UpdatedByRemote:
                        Log.d(Linphone.TAG,"UpdatedByRemote");
                        boolean remoteVideo = false;
                        if(call.getRemoteParams() != null){
                            remoteVideo = call.getRemoteParams().videoEnabled();
                        }
                        boolean localVideo = call.getCurrentParams().videoEnabled();
                        if(remoteVideo != localVideo){
                            findViewById(R.id.toggle_video_button).setSelected(!findViewById(R.id.toggle_video_button).isSelected());
                            Linphone.toggleVideo(remoteVideo);
                            Toast.makeText(getApplicationContext(),"remote updated!",Toast.LENGTH_LONG).show();
                        }
                        // When the remote requests a call update
                        break;
                    case Error:
                        Log.d(Linphone.TAG,"CallState Error");
                        Log.e(Linphone.TAG,message);
                        Log.e(Linphone.TAG, call.getReason().toString());
                        break;
                    case Pausing:
                        findViewById(R.id.pauseButton).setSelected(true);
                        Log.d(Linphone.TAG,"Pausing");
                        break;
                    case Resuming:
                        findViewById(R.id.pauseButton).setSelected(false);
                        Log.d(Linphone.TAG,"Resuming");
                        break;
                    case Idle:
                        Log.d(Linphone.TAG,"Idle");
                        break;
                    case OutgoingInit:
                        Log.d(Linphone.TAG,"OutgoingInit");
                        break;
                    case OutgoingRinging:
                        Log.d(Linphone.TAG,"OutgoingRinging");
                        break;
                    case OutgoingProgress:
                        Log.d(Linphone.TAG,"OutgoingProgress");
                        break;
                    case OutgoingEarlyMedia:
                        Log.d(Linphone.TAG,"OutgoingEarlyMedia");
                        findViewById(R.id.numbpad_button).setVisibility(View.VISIBLE);
                        break;
                    case IncomingEarlyMedia:
                        Log.d(Linphone.TAG,"IncomingEarlyMedia");
                        break;
                    case PushIncomingReceived:
                        Log.d(Linphone.TAG,"PushIncomingReceived");
                        break;
                    case EarlyUpdatedByRemote:
                        Log.d(Linphone.TAG,"EarlyUpdatedByRemote");
                        break;
                    case EarlyUpdating:
                        Log.d(Linphone.TAG,"EarlyUpdating");
                        break;
                    case StreamsRunning:
                        Log.d(Linphone.TAG,"StreamsRunning");
                        break;
                }
            }
        });

    }
    public void Call(){

        if(Linphone.core.getCurrentCall() != null) {
            findViewById(R.id.remote_video_surface).setVisibility(View.VISIBLE);
            findViewById(R.id.local_preview_video_surface).setVisibility(View.VISIBLE);

            findViewById(R.id.layout_early).setVisibility(View.GONE);
            findViewById(R.id.ringing_buttons).setVisibility(View.GONE);
            findViewById(R.id.calling_buttons).setVisibility(View.VISIBLE);
            //findViewById(R.id.call_buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_initials).setVisibility(View.GONE);
            findViewById(R.id.numbpad_button).setVisibility(View.VISIBLE);
            findViewById(R.id.call_buttons).setVisibility(View.VISIBLE);

            if (Linphone.DTMFToneInput != null && !Linphone.DTMFToneInput.equals("")){
                findViewById(R.id.custombutton2).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.custombutton2).setVisibility(View.GONE);
            }
            if (Linphone.RESTInput != null && !Linphone.RESTInput.equals("")){
                findViewById(R.id.custombutton1).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.custombutton1).setVisibility(View.GONE);
            }

        }else{
            this.finish();
        }
    }

    public void OugoingCall(){
        if(Linphone.core.getCurrentCall() != null) {

            findViewById(R.id.remote_video_surface).setVisibility(View.GONE);
            findViewById(R.id.local_preview_video_surface).setVisibility(View.GONE);

            String name = Linphone.core.getCurrentCall().getRemoteAddress().getDisplayName();
            ((TextView) findViewById(R.id.contact_name)).setText(name);
            ((TextView) findViewById(R.id.contact_number)).setText(Linphone.core.getCurrentCall().getRemoteAddress().asStringUriOnly());

            ((TextView) findViewById(R.id.initials)).setText(getInitials(name));


            findViewById(R.id.layout_early).setVisibility(View.GONE);
            findViewById(R.id.ringing_buttons).setVisibility(View.GONE);
            findViewById(R.id.calling_buttons).setVisibility(View.VISIBLE);
            //findViewById(R.id.call_buttons).setVisibility(View.GONE);
            findViewById(R.id.layout_initials).setVisibility(View.VISIBLE);
            findViewById(R.id.numbpad_button).setVisibility(View.GONE);
            findViewById(R.id.call_buttons).setVisibility(View.GONE);

        }else{
            this.finish();
        }
    }

    public void IncomingCall(Boolean isVideo){

        if(Linphone.core.getCurrentCall() != null) {
            if (isVideo) {
                findViewById(R.id.remote_video_surface).setVisibility(View.VISIBLE);
                findViewById(R.id.local_preview_video_surface).setVisibility(View.GONE);
                String name = "";
                if(Linphone.core.getCurrentCall().getRemoteContact() == null){
                    name = Linphone.core.getCurrentCall().getRemoteAddress().getDisplayName();
                }else{
                    name = Linphone.core.getCurrentCall().getRemoteContact();
                }
                ((TextView) findViewById(R.id.contact_name_early)).setText(name);
                ((TextView) findViewById(R.id.contact_number_early)).setText(Linphone.core.getCurrentCall().getRemoteAddress().asStringUriOnly());


                findViewById(R.id.layout_early).setVisibility(View.VISIBLE);
                findViewById(R.id.ringing_buttons).setVisibility(View.VISIBLE);
                findViewById(R.id.calling_buttons).setVisibility(View.GONE);
                //findViewById(R.id.call_buttons).setVisibility(View.GONE);
                findViewById(R.id.layout_initials).setVisibility(View.GONE);
                findViewById(R.id.numbpad_button).setVisibility(View.GONE);
                findViewById(R.id.call_buttons).setVisibility(View.GONE);
            } else {
                findViewById(R.id.remote_video_surface).setVisibility(View.GONE);
                findViewById(R.id.local_preview_video_surface).setVisibility(View.GONE);

                String name = Linphone.core.getCurrentCall().getRemoteAddress().getDisplayName();
                ((TextView) findViewById(R.id.contact_name)).setText(name);
                ((TextView) findViewById(R.id.contact_number)).setText(Linphone.core.getCurrentCall().getRemoteAddress().asStringUriOnly());

                ((TextView) findViewById(R.id.initials)).setText(getInitials(name));


                findViewById(R.id.layout_early).setVisibility(View.GONE);
                findViewById(R.id.ringing_buttons).setVisibility(View.VISIBLE);
                findViewById(R.id.calling_buttons).setVisibility(View.GONE);
                //findViewById(R.id.call_buttons).setVisibility(View.GONE);
                findViewById(R.id.layout_initials).setVisibility(View.VISIBLE);
                findViewById(R.id.numbpad_button).setVisibility(View.GONE);
                findViewById(R.id.call_buttons).setVisibility(View.GONE);
            }

        }else{
            this.finish();
        }
    }

    public String getInitials(String displayName){
        if(displayName == null) return "";
        if (displayName.isEmpty()) return "";

        String[] split = displayName.toUpperCase().split(" ");
        String initials = "";
        int characters = 0;
        for (int i = 0; i<split.length; i++){
            if (!split[i].isEmpty()) {
                initials += split[i];
                characters += 1;
            }
        }
        return initials;
    }

    public void hangup(int type){
        switch (type){
            case 0:
                Linphone.hangUp();
                break;
            case 1:
                Linphone.terminate();
                break;
        }

        Intent mainAct = new Intent(CallActivity.this, MainActivity.class);
        startActivity(mainAct);
    }

    public void handleKeypadClick(View key){
        char keyPressed =  key.getContentDescription().charAt(0);
        Linphone.core.playDtmf(keyPressed, 1);
        if (Linphone.core.getCurrentCall() != null){
            Linphone.core.getCurrentCall().sendDtmf(keyPressed);
        }
    }

    public void toggleNumpadVisibility(View view) {
        view.setSelected(!view.isSelected());
        findViewById(R.id.numpad).setVisibility(view.isSelected()?View.VISIBLE:View.GONE);
    }

    public void toggleMuteMicrophone(View view) {
        view.setSelected(!view.isSelected());
        Linphone.core.enableMic(!Linphone.core.micEnabled());
    }

    public void toggleSpeaker(View view) {
        view.setSelected(!view.isSelected());
        Linphone.toggleSpeaker();
    }
    public void answerCall(String remoteSipAddress) {

        Address remoteAdress = Linphone.core.interpretUrl(remoteSipAddress);
        Call call = (remoteAdress != null) ? Linphone.core.getCallByRemoteAddress2(remoteAdress) : null;
        if (call == null) {
            org.linphone.core.tools.Log.e("[Notification Broadcast Receiver] Couldn't find call from remote address "+remoteSipAddress);
            return;
        }
        call.accept();
        Call();

    }

    public void answerCall(View view) {
        Call currCall = Linphone.core.getCurrentCall();
        if (currCall != null) {
            currCall.accept();
            Call();
        }
    }
    public void toggleVideo(View view){
        view.setSelected(!view.isSelected());
        if (getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        }
        Linphone.toggleVideo();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            Linphone.core.reloadVideoDevices();
        }
    }

    public void toggleCamera(View view){
        Linphone.toggleCamera();
    }
    public void togglePause(View view){
        view.setSelected(!view.isSelected());
        Linphone.toggleCamera();
    }
    public void customButton1(View view){
        try {
            final JSONObject input = new JSONObject(Linphone.RESTInput);
            int disconnectType = input.getInt("disconnectType");

            int cooldownTime = input.getInt("cooldownTime");
            view.setEnabled(false);
            Timer t = new Timer("reenableCustomButton1", false);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    findViewById(R.id.custombutton1).setEnabled(true);
                }
            },cooldownTime);

            switch (input.getString("method")){
                case "GET":
                    ANRequest.GetRequestBuilder getBuilder = AndroidNetworking.get(input.getString("url"));
                    JSONArray getheaders = input.getJSONArray("headers");
                    for (int i = 0; i < getheaders.length(); i++) {
                        JSONObject header = getheaders.getJSONObject(i);
                        getBuilder.addHeaders(header.getString("key"),header.getString("value"));
                    }
                    getBuilder.build().getAsOkHttpResponse(new OkHttpResponseListener() {
                        @Override
                        public void onResponse(Response response) {
                            try {
                                if (response.isSuccessful()){
                                    int successMessageType = input.getInt("successMessageType");
                                    if (successMessageType == 0){
                                        Log.i(Linphone.TAG,input.getString("successMessageSpec"));
                                        Toast.makeText(getApplicationContext(),input.getString("successMessageSpec"),Toast.LENGTH_LONG).show();
                                    }else{
                                        String successMessageSpec = input.getString("successMessageSpec");
                                        String[] successMessageFullPath = successMessageSpec.split("/");
                                        JSONObject parentObject = new JSONObject(response.body().toString());
                                        JSONArray parentArray = new JSONArray();
                                        int lastParent = 0;
                                        for (String successMessagePath : successMessageFullPath){
                                            int begin = successMessagePath.indexOf("(");
                                            int end = successMessagePath.indexOf(")");
                                            String path = successMessagePath.substring(0,begin-1);
                                            String type = successMessagePath.substring(begin+1,end-1);
                                            switch (type){
                                                case "Int":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getInt(path)));
                                                    Toast.makeText(getApplicationContext(),input.getInt(path),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "Bool":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getBoolean(path)));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(input.getBoolean(path)),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "String":
                                                    Log.i(Linphone.TAG,input.getString(path));
                                                    Toast.makeText(getApplicationContext(),input.getString(path),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "JsonObject":
                                                    if (lastParent == 0){
                                                        parentObject = parentObject.getJSONObject(path);
                                                    }else{
                                                        parentObject = parentArray.getJSONObject(Integer.parseInt(path));
                                                    }
                                                    lastParent = 0;
                                                    break;
                                                case "JsonArray":if (lastParent == 0){
                                                    parentArray = parentObject.getJSONArray(path);
                                                }else{
                                                    parentArray = parentArray.getJSONArray(Integer.parseInt(path));
                                                }
                                                    lastParent = 1;
                                                    break;
                                                case "Long":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getLong(path)));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(input.getLong(path)),Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                        }
                                    }
                                    if(disconnectType == 1 || disconnectType == 3){
                                        hangup(0);
                                    }


                                }else{
                                    int failMessageType = input.getInt("failMessageType");
                                    if (failMessageType == 0){
                                        Log.i(Linphone.TAG,input.getString("failMessageSpec"));
                                        Toast.makeText(getApplicationContext(),input.getString("failMessageSpec"),Toast.LENGTH_LONG).show();
                                    }else{
                                        String failMessageSpec = input.getString("failMessageSpec");
                                        String[] failMessageFullPath = failMessageSpec.split("/");
                                        JSONObject parentObject = new JSONObject(response.body().toString());
                                        JSONArray parentArray = new JSONArray();
                                        int lastParent = 0;
                                        for (String failMessagePath : failMessageFullPath){
                                            int begin = failMessagePath.indexOf("(");
                                            int end = failMessagePath.indexOf(")");
                                            String path = failMessagePath.substring(0,begin-1);
                                            String type = failMessagePath.substring(begin+1,end-1);
                                            switch (type){
                                                case "Int":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getInt(path)));
                                                    Toast.makeText(getApplicationContext(),input.getInt(path),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "Bool":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getBoolean(path)));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(input.getBoolean(path)),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "String":
                                                    Log.i(Linphone.TAG,input.getString(path));
                                                    Toast.makeText(getApplicationContext(),input.getString(path),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "JsonObject":
                                                    if (lastParent == 0){
                                                        parentObject = parentObject.getJSONObject(path);
                                                    }else{
                                                        parentObject = parentArray.getJSONObject(Integer.parseInt(path));
                                                    }
                                                    lastParent = 0;
                                                    break;
                                                case "JsonArray":if (lastParent == 0){
                                                    parentArray = parentObject.getJSONArray(path);
                                                }else{
                                                    parentArray = parentArray.getJSONArray(Integer.parseInt(path));
                                                }
                                                    lastParent = 1;
                                                    break;
                                                case "Long":
                                                    Log.i(Linphone.TAG, String.valueOf(input.getLong(path)));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(input.getLong(path)),Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                        }
                                    }
                                    if(disconnectType == 2 || disconnectType == 3){
                                        hangup(0);
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(Linphone.TAG,e.getLocalizedMessage());
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.e(Linphone.TAG,anError.getErrorBody());
                        }
                    });
                    break;
                case "POST":
                    ANRequest.PostRequestBuilder postBuilder = AndroidNetworking.post(input.getString("url"));

                    postBuilder.addJSONObjectBody(input.getJSONObject("body"));

                    JSONArray headers = input.getJSONArray("headers");
                    for (int i = 0; i < headers.length(); i++) {
                        JSONObject header = headers.getJSONObject(i);
                        postBuilder.addHeaders(header.getString("key"),header.getString("value"));
                    }
                    switch (input.getInt("bodyType")){
                        case 0:
                            postBuilder.addJSONObjectBody(input.getJSONObject("body"));
                            break;
                        case 1:
                            postBuilder.addJSONArrayBody(input.getJSONArray("body"));
                            break;
                    }
                    postBuilder.build().getAsOkHttpResponse(new OkHttpResponseListener() {
                        @Override
                        public void onResponse(Response response) {
                            try {
                                if (response.isSuccessful()){
                                    int successMessageType = input.getInt("successMessageType");
                                    if (successMessageType == 0){
                                        Log.i(Linphone.TAG,input.getString("successMessageSpec"));
                                        Toast.makeText(getApplicationContext(),input.getString("successMessageSpec"),Toast.LENGTH_LONG).show();
                                    }else{
                                        String successMessageSpec = input.getString("successMessageSpec");
                                        String[] successMessageFullPath = successMessageSpec.split("/");
                                        JSONObject parentObject = new JSONObject(response.body().toString());
                                        JSONArray parentArray = new JSONArray();
                                        int lastParent = 0;
                                        for (String successMessagePath : successMessageFullPath){
                                            int begin = successMessagePath.indexOf("(");
                                            int end = successMessagePath.indexOf(")");
                                            String path = successMessagePath.substring(0,begin-1);
                                            String type = successMessagePath.substring(begin+1,end-1);
                                            switch (type){
                                                case "Int":
                                                    int resultInt;
                                                    if (lastParent == 0){
                                                        resultInt = parentObject.getInt(path);
                                                    }else{
                                                        resultInt = parentArray.getInt(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultInt));
                                                    Toast.makeText(getApplicationContext(),resultInt,Toast.LENGTH_LONG).show();
                                                    break;
                                                case "Bool":
                                                    Boolean resultBool;
                                                    if (lastParent == 0){
                                                        resultBool = parentObject.getBoolean(path);
                                                    }else{
                                                        resultBool = parentArray.getBoolean(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultBool));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(resultBool),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "String":
                                                    String resultString;
                                                    if (lastParent == 0){
                                                        resultString = parentObject.getString(path);
                                                    }else{
                                                        resultString = parentArray.getString(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG,resultString);
                                                    Toast.makeText(getApplicationContext(),resultString,Toast.LENGTH_LONG).show();
                                                    break;
                                                case "JsonObject":
                                                    if (lastParent == 0){
                                                        parentObject = parentObject.getJSONObject(path);
                                                    }else{
                                                        parentObject = parentArray.getJSONObject(Integer.parseInt(path));
                                                    }
                                                    lastParent = 0;
                                                    break;
                                                case "JsonArray":if (lastParent == 0){
                                                    parentArray = parentObject.getJSONArray(path);
                                                }else{
                                                    parentArray = parentArray.getJSONArray(Integer.parseInt(path));
                                                }
                                                    lastParent = 1;
                                                    break;
                                                case "Long":
                                                    long resultLong;
                                                    if (lastParent == 0){
                                                        resultLong = parentObject.getLong(path);
                                                    }else{
                                                        resultLong = parentArray.getLong(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultLong));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(resultLong),Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                        }
                                    }
                                    if(disconnectType == 1 || disconnectType == 3){
                                        hangup(0);
                                    }


                                }else{
                                    int failMessageType = input.getInt("failMessageType");
                                    if (failMessageType == 0){
                                        Log.i(Linphone.TAG,input.getString("failMessageSpec"));
                                        Toast.makeText(getApplicationContext(),input.getString("failMessageSpec"),Toast.LENGTH_LONG).show();
                                    }else{
                                        String failMessageSpec = input.getString("failMessageSpec");
                                        String[] failMessageFullPath = failMessageSpec.split("/");
                                        JSONObject parentObject = new JSONObject(response.body().toString());
                                        JSONArray parentArray = new JSONArray();
                                        int lastParent = 0;
                                        for (String failMessagePath : failMessageFullPath){
                                            int begin = failMessagePath.indexOf("(");
                                            int end = failMessagePath.indexOf(")");
                                            String path = failMessagePath.substring(0,begin-1);
                                            String type = failMessagePath.substring(begin+1,end-1);
                                            switch (type){
                                                case "Int":
                                                    int resultInt;
                                                    if (lastParent == 0){
                                                        resultInt = parentObject.getInt(path);
                                                    }else{
                                                        resultInt = parentArray.getInt(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultInt));
                                                    Toast.makeText(getApplicationContext(),resultInt,Toast.LENGTH_LONG).show();
                                                    break;
                                                case "Bool":
                                                    Boolean resultBool;
                                                    if (lastParent == 0){
                                                        resultBool = parentObject.getBoolean(path);
                                                    }else{
                                                        resultBool = parentArray.getBoolean(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultBool));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(resultBool),Toast.LENGTH_LONG).show();
                                                    break;
                                                case "String":
                                                    String resultString;
                                                    if (lastParent == 0){
                                                        resultString = parentObject.getString(path);
                                                    }else{
                                                        resultString = parentArray.getString(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG,resultString);
                                                    Toast.makeText(getApplicationContext(),resultString,Toast.LENGTH_LONG).show();
                                                    break;
                                                case "JsonObject":
                                                    if (lastParent == 0){
                                                        parentObject = parentObject.getJSONObject(path);
                                                    }else{
                                                        parentObject = parentArray.getJSONObject(Integer.parseInt(path));
                                                    }
                                                    lastParent = 0;
                                                    break;
                                                case "JsonArray":if (lastParent == 0){
                                                    parentArray = parentObject.getJSONArray(path);
                                                }else{
                                                    parentArray = parentArray.getJSONArray(Integer.parseInt(path));
                                                }
                                                    lastParent = 1;
                                                    break;
                                                case "Long":
                                                    long resultLong;
                                                    if (lastParent == 0){
                                                        resultLong = parentObject.getLong(path);
                                                    }else{
                                                        resultLong = parentArray.getLong(Integer.parseInt(path));
                                                    }
                                                    Log.i(Linphone.TAG, String.valueOf(resultLong));
                                                    Toast.makeText(getApplicationContext(),String.valueOf(resultLong),Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                        }
                                    }
                                    if(disconnectType == 2 || disconnectType == 3){
                                        hangup(0);
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(Linphone.TAG,e.getLocalizedMessage());
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.e(Linphone.TAG,anError.getErrorBody());
                        }
                    });
                    break;
            }
        }catch (JSONException e) {
            e.printStackTrace();
            Log.e(Linphone.TAG,e.getLocalizedMessage());
        }
    }
    public void customButton2(View view) {
        try {
            JSONObject input = new JSONObject(Linphone.DTMFToneInput);
            int cooldownTime = input.getInt("cooldownTime");
            view.setEnabled(false);
            Timer t = new Timer("reenableCustomButton2", false);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    findViewById(R.id.custombutton2).setEnabled(true);
                }
            },cooldownTime);
            if (Linphone.core.getCurrentCall() != null) {
                try {
                    String sequence = input.getString("sequence");
                    for (char key : sequence.toCharArray()) {
                        if (key == ',') {
                            Thread.sleep(1000);
                            continue;
                        }
                        Linphone.core.playDtmf(key, 1);
                        Linphone.core.getCurrentCall().sendDtmf(key);
                    }
                } catch(InterruptedException e){
                    e.printStackTrace();
                    Log.e(Linphone.TAG,input.getString("failMessage"));
                    Toast.makeText(getApplicationContext(),input.getString("failMessage"),Toast.LENGTH_LONG).show();
                }
            }else{
                Log.i(Linphone.TAG,input.getString("successMessage"));
                Toast.makeText(getApplicationContext(),input.getString("successMessage"),Toast.LENGTH_LONG).show();
            }
        }catch(JSONException e){
            e.printStackTrace();
            Log.e(Linphone.TAG,e.getLocalizedMessage());
        }
    }


    public void hangupCall(View view) {
        hangup(1);
    }

    public void TerminateCall(View view) {
        hangup(0);
    }
}