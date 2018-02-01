package me.chrismarkus.digital_sputnik_clone;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    intensity masterIntens = new intensity();
    color masterColor = new color();
    ToggleButton lightSwitch;

    ToggleButton presetButtons[] = new ToggleButton[8];
    Preset[] presets = new Preset[8];

    ToggleButton animButtons[] = new ToggleButton[8];
    Animation[] animations = new Animation[4];
    MessageThread messageThread = new MessageThread();

    PresetAnimation presetAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the content view
        setContentView(R.layout.activity_main);
        //lightSwitch:
        lightSwitch = (ToggleButton)findViewById(R.id.lightSwitch);
        lightSwitch.setOnLongClickListener(longClickListener);


        //presetAnimations:
        presetAnimation = new PresetAnimation();
        //defaults:
        snapClick(findViewById(R.id.snapButton));

        //create a shared preferences handle:
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

        //create a resource handle
        Resources res = getResources();
        for(int i=1;i<=8;i++){
            //Preset setup:
            String temp = "preset_" + i;
            presetButtons[i-1] = (ToggleButton)findViewById(res.getIdentifier(temp, "id",getPackageName()));
            presetButtons[i-1].setOnLongClickListener(longClickListener);
            presets[i-1] = new Preset();
            String rawPreset = sharedPreferences.getString("preset_"+i, "255,255,255");
            int r = Integer.parseInt(rawPreset.split(",")[0]);
            int g = Integer.parseInt(rawPreset.split(",")[1]);
            int b = Integer.parseInt(rawPreset.split(",")[2]);
            presets[i-1].setPreset(r,g,b);
        }
        for(int i=1; i<=4; i++){
            //animation setup:
            String animTemp = "anim_" + i;
            animButtons[i-1] = (ToggleButton)findViewById(res.getIdentifier(animTemp, "id",getPackageName()));
            animations[i-1] = new Animation();
        }

        //sliders, eventually put this in a loop
        SeekBar slider1 = (SeekBar)findViewById(R.id.slider1);
        slider1.setOnSeekBarChangeListener(new seekListener());
        slider1.setOnLongClickListener(longClickListener);

        SeekBar slider2 = (SeekBar)findViewById(R.id.slider2);
        slider2.setOnSeekBarChangeListener(new seekListener());

        SeekBar slider3 = (SeekBar)findViewById(R.id.slider3);
        slider3.setOnSeekBarChangeListener(new seekListener());

        SeekBar slider4 = (SeekBar)findViewById(R.id.slider4);
        masterIntens.setSlider(slider4);
        slider4.setOnSeekBarChangeListener(new seekListener());

        SeekBar slider5 = (SeekBar)findViewById(R.id.slider5);
        slider5.setOnSeekBarChangeListener(new seekListener());

        masterColor.setRed(0);

        messageThread.initMessageThread();



    }

    //Class definition for seekListener
    private class seekListener implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            String extra = "";
            Resources res = getResources();
            String barId = res.getResourceEntryName(seekBar.getId()) + "_text";
            TextView tv = (TextView)findViewById(res.getIdentifier(barId, "id",getPackageName()));
            String sliderVal = res.getString(res.getIdentifier(barId.split("_")[0], "string",getPackageName()));
            if(seekBar.getId() == R.id.slider5){
                progress += presetAnimation.getMin();
                extra = " sec";
            }
            tv.setText(sliderVal+": "+progress + extra);
            sendSeek(seekBar, progress, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar){
        }
    }
    private class intensity{
        int intensity = 0;
        int lastIntensity = 0;
        SeekBar slider;
        public int getLastIntensity(){
            return lastIntensity;
        }
        public int getIntensity(){
            return intensity;
        }
        public void setSlider(SeekBar s){
            slider = s;
        }
        public SeekBar getSlider(){
            return slider;
        }
        public void setIntensity(int intens){
            intensity = intens;
            if(intens !=0){
                lastIntensity = intens;
            }
            masterColor.setIntens(intensity);
        }
    }
    private class color{
        int red = 0;
        int green = 0;
        int blue = 0;
        int maxAmt = 255;
        int maxIntens = 255;
        public void setIntens(int intens){
            //postData("r", red * masterIntens.getIntensity()/maxIntens);
            //postData("g", green * masterIntens.getIntensity()/maxIntens);
            //postData("b", blue * masterIntens.getIntensity()/maxIntens);
            writeColor();
            displayColor();
        }
        public void setRed(int amt){
            red = amt;
            if(amt > maxAmt){
                red = maxAmt;
            }
            if(masterIntens.getIntensity() > 0) {
                writeColor();
                //postData("r", red * masterIntens.getIntensity() / maxIntens);
            }
            displayColor();
        }
        public void setGreen(int amt){
            green = amt;
            if(amt > maxAmt){
                green = maxAmt;
            }
            if(masterIntens.getIntensity() > 0) {
                writeColor();
                //postData("g", green * masterIntens.getIntensity() / maxIntens);
            }
            displayColor();
        }
        public void setBlue(int amt){
            blue = amt;
            if(amt > maxAmt){
                blue = maxAmt;
            }
            if(masterIntens.getIntensity() > 0) {
                writeColor();
                //postData("b", blue * masterIntens.getIntensity() / maxIntens);
            }
            displayColor();
        }
        private void writeColor(){
            int r = red * masterIntens.getIntensity() / maxIntens;
            int g = green * masterIntens.getIntensity() / maxIntens;
            int b = blue * masterIntens.getIntensity() / maxIntens;
            postData(toHex(r) + toHex(g) + toHex(b));
        }

        public int getRed(){
            return red;
        }
        public int getGreen(){
            return green;
        }
        public int getBlue(){
            return blue;
        }
        private void displayColor(){
            View colorDisplay = (View)findViewById(R.id.color_display);
            //TextView colorText = (TextView)findViewById(R.id.color_text);
            /*int textColor;
            if((red+green+blue)/3 > 130){
                textColor = Color.rgb(0,0,0);
            }
            else{
                textColor = Color.rgb(255,255,255);
            }*/
            colorDisplay.setBackgroundColor(Color.rgb(red,green,blue));
            //colorText.setTextColor(textColor);
            //colorText.setText("#"+toHex(red)+toHex(green)+toHex(blue));
        }
    }

    //menu button:
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public String getMasterColorHex(){
        return toHex(masterColor.getRed() + masterColor.getGreen() + masterColor.getBlue());
    }

    private class simpleColor {
        int red = 255; int blue=255; int green = 255;
        simpleColor(int r, int g, int b) {
            this.red = r;
            this.blue = b;
            this.green = g;
        }
        public int getRed() {
            return this.red;
        }
        public int getBlue() {
            return this.red;
        }
        public int getGreen() {
            return this.red;
        }
        public void setRed(int r) {
            this.red = r;
            if(r > 255){
                this.red = 255;
            }
            else if(r > 0){
                this.red = 0;
            }
        }
        public void setBlue(int b) {
            this.blue = b;
            if(b > 255){
                this.blue = 255;
            }
            else if(b > 0){
                this.blue = 0;
            }
        }
        public void setGreen(int g) {
            this.green = g;
            if(g > 255){
                this.green = 255;
            }
            else if(g > 0){
                this.green = 0;
            }
        }
    }

    private class Preset{
        int maxColor = 255;
        int red;
        int blue;
        int green;

        public Preset(){
            this.red = maxColor;
            this.blue = maxColor;
            this.green = maxColor;
        }

        public void setPreset(int r, int g, int b){
            this.red = r;
            this.green = g;
            this.blue = b;
            if(r > maxColor){
                this.red = maxColor;
            }
            if(g > maxColor){
                this.green = maxColor;
            }
            if(g > maxColor){
                this.green = maxColor;
            }
        }
        public int getRed(){
            return this.red;
        }
        public int getBlue(){
            return this.blue;
        }
        public int getGreen(){
            return this.green;
        }
    }

    private class Animation {
        LinkedList<simpleColor> keyframes = new LinkedList<simpleColor>();
        LinkedList<Integer> timing = new LinkedList<Integer>();
        public void snap() {

        }
        public void fade() {

        }
        public void strobe() {

        }
    }

    public void snapClick(View v){
        presetAnimation.setSnap();
    }

    public void fadeClick(View v){
        presetAnimation.setFade();
    }

    private class PresetAnimation{
        boolean hardStopAnimation = false;
        boolean snap = true;
        int minDuration = 1;

        ToggleButton snapButton = (ToggleButton)findViewById(R.id.snapButton);
        ToggleButton fadeButton = (ToggleButton)findViewById(R.id.fadeButton);

        SeekBar duration = (SeekBar)findViewById(R.id.slider5);
        TextView readout = (TextView)findViewById(R.id.slider5_text);

        public void setSnap(){
            snap = true;
            snapButton.setChecked(true);
            fadeButton.setChecked(false);
            duration.setVisibility(View.GONE);
            readout.setVisibility(View.GONE);
        }

        public void setFade(){
            snap = false;
            snapButton.setChecked(false);
            fadeButton.setChecked(true);
            duration.setVisibility(View.VISIBLE);
            readout.setVisibility(View.VISIBLE);
        }

        public void cancelAnimation(){
            this.hardStopAnimation = true;
        }

        public boolean getSnap(){
            return snap;
        }

        public int getMin(){
            return minDuration;
        }

        public void animate(int r, int g, int b){
            //define start and end colors:
            simpleColor start = new simpleColor(masterColor.getRed(), masterColor.getBlue(), masterColor.getGreen());
            simpleColor end = new simpleColor(r, g, b);
            //get duration:
            SeekBar slide = ((SeekBar)findViewById(R.id.slider5));
            int duration = slide.getProgress() + minDuration;
            //calculate and run:
            int steps = Math.abs(start.getRed() - end.getRed());
            int gSteps = end.getGreen() - start.getGreen();
            int rSteps = end.getRed() - start.getRed();
            int bSteps = end.getBlue() - start.getBlue();
            float rDub = (float)start.getRed();
            float gDub = (float)start.getGreen();
            float bDub = (float)start.getBlue();
            if(Math.abs(gSteps) > steps){
                steps = Math.abs(gSteps);
            }
            if(Math.abs(bSteps) > steps){
                steps = Math.abs(bSteps);
            }
            for(int i=0; i<steps; i++){
                if(this.hardStopAnimation){
                    this.hardStopAnimation = false;
                    return;
                }
                rDub += (((float)rSteps)/((float)steps));
                gDub += (((float)gSteps)/((float)steps));
                bDub += (((float)bSteps)/((float)steps));
                masterColor.setRed((int)rDub);
                ((SeekBar) findViewById(R.id.slider1)).setProgress((int)rDub);
                masterColor.setGreen((int)gDub);
                ((SeekBar) findViewById(R.id.slider2)).setProgress((int)gDub);
                masterColor.setBlue((int)bDub);
                ((SeekBar) findViewById(R.id.slider3)).setProgress((int)bDub);
                Log.d("Color", (int)rDub + "");
                try
                {
                    Thread.sleep(Math.abs(duration / steps));
                }
                catch(InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
            //make sure we actually got to the right color:
            masterColor.setRed(end.getRed());
            ((SeekBar) findViewById(R.id.slider1)).setProgress(end.getRed());
            masterColor.setBlue(end.getBlue());
            ((SeekBar) findViewById(R.id.slider2)).setProgress(end.getBlue());
            masterColor.setGreen(end.getGreen());
            ((SeekBar) findViewById(R.id.slider3)).setProgress(end.getGreen());
        }
    }


    View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v){
            longClickHandler(v);
            return true;
        }
    };

    public void longClickHandler(View v){
        switch(v.getId()){
            case R.id.lightSwitch:
                SeekBar temp = masterIntens.getSlider();
                masterIntens.setIntensity(temp.getMax());
                temp.setProgress(temp.getMax());
                lightSwitch.setChecked(true);
                break;
            case R.id.slider1:
                SeekBar slider = (SeekBar)v;
                slider.setProgress(slider.getMax());
                break;
            case R.id.preset_1:
            case R.id.preset_2:
            case R.id.preset_3:
            case R.id.preset_4:
            case R.id.preset_5:
            case R.id.preset_6:
            case R.id.preset_7:
            case R.id.preset_8:
                savePreset(v);
                break;
        }
    }

    public void toggleLight(View v){
        int intens = 0;
        ToggleButton t = (ToggleButton)v;
        if(t.isChecked()){
            int lastIntens = masterIntens.getLastIntensity();
            intens = lastIntens;
            if(lastIntens == 0){
                intens = masterIntens.getSlider().getMax();
            }
        }
        else{
            intens = 0;
        }
        masterIntens.setIntensity(intens);
        masterIntens.getSlider().setProgress(intens);
    }

    public void selectPreset(View v){
        Resources res = getResources();
        String presetId = res.getResourceEntryName(v.getId());
        //get index number of preset
        int index = Integer.parseInt(presetId.split("_")[1]) - 1;
        for(int i=0;i<8;i++){
            if(i != index){
                presetButtons[i].setChecked(false);
            }
            else{
                presetButtons[i].setChecked(true);
            }
        }
        if(presetAnimation.getSnap()) {
            masterColor.setRed(presets[index].getRed());
            ((SeekBar) findViewById(R.id.slider1)).setProgress(presets[index].getRed());
            masterColor.setGreen(presets[index].getGreen());
            ((SeekBar) findViewById(R.id.slider2)).setProgress(presets[index].getGreen());
            masterColor.setBlue(presets[index].getBlue());
            ((SeekBar) findViewById(R.id.slider3)).setProgress(presets[index].getBlue());
        }
        else{
            presetAnimation.animate(presets[index].getRed(), presets[index].getGreen(), presets[index].getBlue());
        }
    }

    public void savePreset(View v) {
        //resource handle:
        Resources res = getResources();
        //create a shared preferences handle:
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        //get the friendly id:
        String presetId = res.getResourceEntryName(v.getId());
        //get index number of preset
        int index = Integer.parseInt(presetId.split("_")[1]) - 1;
        presets[index].setPreset(masterColor.getRed(), masterColor.getGreen(), masterColor.getBlue());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("preset_"+(index+1), masterColor.getRed()+","+masterColor.getGreen()+","+masterColor.getBlue());
        editor.commit();
        presetButtons[index].setChecked(true);
    }

    public void selectAnim(View v){
        Resources res = getResources();
        String animId = res.getResourceEntryName(v.getId());
        //get index number of preset
        int index = Integer.parseInt(animId.split("_")[1]) - 1;
        for(int i=0;i<4;i++){
            if(i != index){
                animButtons[i].setChecked(false);
            }
            else{
                animButtons[i].setChecked(true);
            }
        }
        masterColor.setRed(presets[index].getRed());
        ((SeekBar)findViewById(R.id.slider1)).setProgress(presets[index].getRed());
        masterColor.setGreen(presets[index].getGreen());
        ((SeekBar)findViewById(R.id.slider2)).setProgress(presets[index].getGreen());
        masterColor.setBlue(presets[index].getBlue());
        ((SeekBar)findViewById(R.id.slider3)).setProgress(presets[index].getBlue());
    }

    public void sendSeek(SeekBar s, int val, boolean user){
        switch(s.getId()){
            case R.id.slider1:
                masterColor.setRed(val);
                break;
            case R.id.slider2:
                masterColor.setGreen(val);
                break;
            case R.id.slider3:
                masterColor.setBlue(val);
                break;
            case R.id.slider4:
                if(user){
                    masterIntens.setIntensity(val);
                    if(val > 0){
                        lightSwitch.setChecked(true);
                    }
                    else{
                        lightSwitch.setChecked(false);
                    }
                }
                break;
        }
        //uncheck preset buttons:
        if(user && s.getId() != R.id.slider4){
            for(int i=0;i<8;i++){
                presetButtons[i].setChecked(false);
            }
        }

        //cancel animation if from user:
        if(user) {
            presetAnimation.cancelAnimation();
        }

    }

    public void fadeTo(simpleColor start, simpleColor end, int duration){
        int steps = Math.abs(start.getRed() - end.getRed());
        int gSteps = end.getGreen() - start.getGreen();
        int rSteps = end.getRed() - start.getRed();
        int bSteps = end.getBlue() - start.getBlue();
        float rDub = (float)start.getRed();
        float gDub = (float)start.getGreen();
        float bDub = (float)start.getBlue();
        if(Math.abs(gSteps) > steps){
            steps = Math.abs(gSteps);
        }
        if(Math.abs(bSteps) > steps){
            steps = Math.abs(bSteps);
        }
        for(int i=0; i<steps; i++){
            rDub += (((float)rSteps)/((float)steps));
            gDub += (((float)gSteps)/((float)steps));
            bDub += (((float)bSteps)/((float)steps));
            masterColor.setRed((int)rDub);
            ((SeekBar) findViewById(R.id.slider1)).setProgress((int)rDub);
            masterColor.setGreen((int)gDub);
            ((SeekBar) findViewById(R.id.slider2)).setProgress((int)gDub);
            masterColor.setBlue((int)bDub);
            ((SeekBar) findViewById(R.id.slider3)).setProgress((int)bDub);
            try
            {
                Thread.sleep(Math.abs(duration / steps));
            }
            catch(InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        }
        //make sure we actually got to the right color:
        masterColor.setRed(end.getRed());
        ((SeekBar) findViewById(R.id.slider1)).setProgress(end.getRed());
        masterColor.setBlue(end.getBlue());
        ((SeekBar) findViewById(R.id.slider2)).setProgress(end.getBlue());
        masterColor.setGreen(end.getGreen());
        ((SeekBar) findViewById(R.id.slider3)).setProgress(end.getGreen());
    }

    public String toHex(int inpt) {
        String opt = Integer.toHexString(inpt);
        if(opt.length() == 1){
            opt = "0"+opt;
        }
        return opt;
    }

    public void postData(String data){
        if(messageThread.getStarted()) {
            messageThread.send(data);
        }
        else{
            //alertNoButton("No Connection!");
        }
    }

    public void restartMessageThread(){
        if(!messageThread.getStarted()) {
            messageThread.initMessageThread();
        }
    }



    private class MessageThread {
        final Handler handler = new Handler();
        private volatile Thread thread;
        String msg  = "";
        boolean kill = false;
        PrintWriter output;
        BufferedReader input;
        OutputStream out;
        Socket s;
        String address = "192.168.1.1";
        boolean restart = false;
        boolean socketStarted = false;
        public void initMessageThread(){
            Log.d("Spot", "1");
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if(!socketStarted) {
                        kill = true;
                    }
                    try {
                        //Replace below IP with the IP of that device in which server socket open.
                        //If you change port then change the port number in the server side code also.
                        try {
                            s = new Socket(address, 21);
                            Log.d("Spot", s.getInetAddress()+"");
                            kill = false;
                        } catch(SocketException se){
                            Log.d("Spot ", kill + "");
                            socketStarted = false;
                            try {
                                thread.sleep(5000);
                            } catch(InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            alertNoButton("No Connection... Retrying...");
                            restartMessageThread();
                            thread = null;
                        }
                        if(!kill && s != null) {
                            out = s.getOutputStream();

                            output = new PrintWriter(out);

                            input = new BufferedReader(new InputStreamReader(s.getInputStream()));

                            socketStarted = true;
                            alertNoButton("Connected!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    while(!kill){
                        if(msg != "") {
                            output.println(msg);
                            output.flush();
                            msg = "";
                        }
                        //if we aren't connected:
                        if(!s.isConnected()){
                            alertConnectionDie();
                            kill = true;
                            thread = null;
                            //restart = true;
                        }
                        /*
                        try {
                            final String st = input.readLine();

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    /*String s = ((TextView)findViewById(R.id.postData)).getText().toString();
                                    if (st.trim().length() != 0)
                                    ((TextView)findViewById(R.id.postData)).setText(s + "\nFrom Server : " + st);
                                }
                            });
                        } catch(IOException e){
                            e.printStackTrace();
                        }*/
                        /*if(s.getInetAddress() + "" != "/"+address){
                            kill = true;
                            restartMessageThread();
                            thread = null;
                        }*/
                        try {
                            thread.sleep(6);
                        } catch(InterruptedException ex){
                            ex.printStackTrace();
                        }
                    }
                    try {
                        if(socketStarted) {
                            output.close();
                            out.close();
                            s.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //if(restart){
                        //restartMessageThread();
                    //}
                }
            });
            thread.start();
        }
        public void kill(){
            kill = true;
        }
        public void send(final String message) {
            this.msg = message;
        }
        public boolean getStarted(){
            return socketStarted;
        }
    }

    public void alertConnectionDie() {
        String msg = "Connection Lost!";
        String button = "Retry";
        final CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.snackbar_container);
        Snackbar snackbar = Snackbar
                .make(layout, msg, Snackbar.LENGTH_LONG)
                .setAction(button, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar snackbar1 = Snackbar.make(layout, "Connecting...", Snackbar.LENGTH_SHORT);
                        messageThread.initMessageThread();
                    }
                });

        snackbar.show();
    }

    public void alertNoButton(String msg){
        final CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.snackbar_container);
        Snackbar snackbar = Snackbar
                .make(layout, msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void fabClick(View v){
        Intent intent = new Intent(this, Animations.class);
        startActivity(intent);
    }
/*
    private void sendMessage(final String msg) {
        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //Replace below IP with the IP of that device in which server socket open.
                    //If you change port then change the port number in the server side code also.
                    Socket s = new Socket("192.168.1.1", 21);

                    OutputStream out = s.getOutputStream();

                    PrintWriter output = new PrintWriter(out);

                    output.println(msg);
                    output.flush();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String st = input.readLine();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            /*String s = ((TextView)findViewById(R.id.postData)).getText().toString();
                            if (st.trim().length() != 0)
                                ((TextView)findViewById(R.id.postData)).setText(s + "\nFrom Server : " + st);
                        }
                    });

                    output.close();
                    out.close();
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }*/
}