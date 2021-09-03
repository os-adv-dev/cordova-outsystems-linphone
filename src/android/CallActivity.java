package com.outsystems.linphone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import $appid.MainActivity;
import $appid.R;

import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.Response;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseListener;


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
                Linphone.outgoingCall(domain,outVideo,lowBandwidth);
                OugoingCall();
                break;
            case "Ringing":
                Boolean inVideo = currIntent.getBooleanExtra("Video",false);
                IncomingCall(inVideo);
                break;
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

    public void hangup(){
        Linphone.hangUp();
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

    public void answerCall(View view) {
        Call currCall = Linphone.core.getCurrentCall();
        if (currCall != null) {
            currCall.accept();
            Call();
        }
    }
    public void toggleVideo(View view){
        view.setSelected(!view.isSelected());
        Linphone.toggleVideo();
    }
    public void toggleCamera(View view){
        Linphone.toggleCamera();
    }
    public void togglePause(View view){
        view.setSelected(!view.isSelected());
        Linphone.toggleCamera();
    }
    public void customButton1(View view){
        //todo finish
        try {
            final JSONObject input = new JSONObject(Linphone.RESTInput);
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
                                    Log.i(Linphone.TAG,input.getString("successMessage"));
                                    Toast.makeText(getApplicationContext(),input.getString("successMessage"),Toast.LENGTH_LONG).show();

                                }else{
                                    Log.e(Linphone.TAG,input.getString("failMessage"));
                                    Toast.makeText(getApplicationContext(),input.getString("failMessage"),Toast.LENGTH_LONG).show();
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
                    postBuilder.build().getAsOkHttpResponse(new OkHttpResponseListener() {
                        @Override
                        public void onResponse(Response response) {
                            try {
                                if (response.isSuccessful()){
                                    Log.i(Linphone.TAG,input.getString("successMessage"));
                                    Toast.makeText(getApplicationContext(),input.getString("successMessage"),Toast.LENGTH_LONG).show();

                                }else{
                                    Log.e(Linphone.TAG,input.getString("failMessage"));
                                    Toast.makeText(getApplicationContext(),input.getString("failMessage"),Toast.LENGTH_LONG).show();
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
        //todo finish
        try {
            JSONObject input = new JSONObject(Linphone.DTMFToneInput);

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
        hangup();
    }
}