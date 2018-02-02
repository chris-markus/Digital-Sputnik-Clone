#include <SoftwareSerial.h>
#include <stdlib.h>

//color vars:
int r;
int g;
int b;

//pins:
const int R_PIN = 9;
const int G_PIN = 10;
const int B_PIN = 11;
const int V_PIN = A1;
const int RX_PIN = 8;
const int TX_PIN = 7;
const int led = LED_BUILTIN;
const int PACKET_LENGTH = 6;
const int BAUD_RATE = 9600;

bool hardStop = false;

bool lowVolt = false;

int incomingByte = 0;

float cutoffVoltage = 6.3;

//char arrays
char rawData[6];
char color[6];

//setup software serial:
SoftwareSerial wifiInterface(RX_PIN, TX_PIN); // RX, TX

void setup() {
  
  //set pinmodes:
  pinMode(R_PIN, OUTPUT);
  pinMode(G_PIN, OUTPUT);
  pinMode(B_PIN, OUTPUT);
  pinMode(V_PIN, INPUT);
  pinMode(RX_PIN, INPUT);
  pinMode(TX_PIN, OUTPUT);
  pinMode(led, OUTPUT);

  //avoid LED flicker on video by raising PWM rate
  TCCR1B = (TCCR1B & 0b11111000 | 0x01);
  TCCR2B = (TCCR2B & 0b11111000 | 0x01);

  //start serial connections:
  Serial.begin(9600);
  wifiInterface.begin(BAUD_RATE);

  //let the user know we're ready
  startupAnim();
}

void loop() {
  // put your main code here, to run repeatedly:
  //readColor();
  if(readColor()){
    int red, green, blue;
    if(hexToInts(&red, &green, &blue, color)){
      snapTo(red, green, blue);
      Serial.println(r);
      Serial.println(g);
      Serial.println(b);
    }
  }
}

boolean readColor(){
  int dataCounter = 0;
  char incomingByte;
  bool stop = false;
  while(!stop){
    if(wifiInterface.available()){
      incomingByte = wifiInterface.read();
      if(incomingByte == 10){
        if(dataCounter == PACKET_LENGTH && verifyColor()){
          for(int i=0; i<PACKET_LENGTH; i++){
            color[i] = rawData[i];
          }
          return true;
        }
        else{
          return false;
        }
      }
      else{
        rawData[dataCounter] = incomingByte;
        dataCounter ++;
      }
    }
  }
}


void startupAnim(){
  int del = 1;
  for(int o = 0; o < 2; o++){
    for(int i = 0; i<255; i++){
      analogWrite(G_PIN, i);
      delay(del);
    }
    for(int i=255;i>=0;i--){
      analogWrite(G_PIN, i);
      delay(del);
    }
  }
}

void errorAnim(){
  int del = 300;
  for(int i=0;i<3;i++){
    analogWrite(G_PIN, 0);
    analogWrite(B_PIN, 0);
    analogWrite(R_PIN, 255);
    delay(del);
    analogWrite(G_PIN, g);
    analogWrite(B_PIN, b);
    analogWrite(R_PIN, r);
    delay(del);
  }
}

void fadeTo(int red, int green, int blue, int duration){
  int steps = abs(r - red);
  int gSteps = green - g;
  int rSteps = red - r;
  int bSteps = blue - b;
  float rDub = (float)r;
  float gDub = (float)g;
  float bDub = (float)b;
  if(abs(gSteps) > steps){
    steps = abs(gSteps);
  }
  if(abs(bSteps) > steps){
    steps = abs(bSteps);
  }
  for(int i=0; i<steps; i++){
    if(hardStop){
      hardStop = false;
      break;
    }
    rDub += (((float)rSteps)/((float)steps));
    gDub += (((float)gSteps)/((float)steps));
    bDub += (((float)bSteps)/((float)steps));
    r = (int)rDub;
    g = (int)gDub;
    b = (int)bDub;
    writeColor();
    delay(abs(duration / steps));
  }
  //make sure we actually got to the right color:
  r = red;
  g = green;
  b = blue;
  writeColor();
}

void writeColor(){
  if(!lowVolt){
    analogWrite(R_PIN, r);
    analogWrite(G_PIN, g);
    analogWrite(B_PIN, b);
  }
  else{
    analogWrite(R_PIN, 0);
    analogWrite(G_PIN, 0);
    analogWrite(B_PIN, 0);
  }
}

void snapTo(int red, int green, int blue){
  r = red;
  g = green;
  b = blue;
  writeColor();
}

float checkVoltage(){
  float voltage = 2 * (5.0 / 1023.0) * analogRead(V_PIN);
  if(voltage < cutoffVoltage){
    lowVolt = true;
    writeColor();
    //delay forever:
    delay(9999999999999999);
  }
  return voltage;
}

boolean verifyColor(){
  for(int i=0; i<PACKET_LENGTH; i++){
    //a little confusing, but checks if ascii values are 0-9 or a-f, returns false if not
    if(!((rawData[i] >= '0' && rawData[i] <= '9') || (rawData[i] >= 'a' && rawData[i] <= 'f'))){
      return false;
    }
  }
  return true;
}

boolean hexToInts(int* redP, int* greenP, int* blueP, char* inColor){
  int tempR, tempG, tempB;
  sscanf(color, "%02x%02x%02x", &tempR, &tempG, &tempB);
  if(tempR >= 0 && tempR <= 255 &&
     tempG >= 0 && tempG <= 255 &&
     tempB >= 0 && tempB <= 255){ 
    *redP = tempR;
    *greenP = tempG;
    *blueP = tempB;
    return true;
  }
  else{
    return false;
  }
}
