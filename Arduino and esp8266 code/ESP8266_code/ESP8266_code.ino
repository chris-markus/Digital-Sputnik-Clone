#include <ESP8266WiFi.h>

#define MAX_SRV_CLIENTS 3

const char* ssid = "Voyager1";
const char* password = "lightbar";

IPAddress local_IP(192,168,1,1);
IPAddress gateway(192,168,1,1);
IPAddress subnet(255,255,255,0);

WiFiServer server(21);
WiFiClient serverClient;

void setup()
{
  Serial.begin(9600);
  Serial.println();

  WiFi.mode(WIFI_AP);
  WiFi.begin();

  Serial.print("Setting soft-AP configuration ... ");
  Serial.println(WiFi.softAPConfig(local_IP, gateway, subnet) ? "Ready" : "Failed!");
  
  Serial.print("Setting soft-AP ... ");
  Serial.println(WiFi.softAP(ssid) ? "Ready" : "Failed!");

  Serial.print("Soft-AP IP address = ");
  Serial.println(WiFi.softAPIP());

  server.begin();
  server.setNoDelay(true);
}
 
void loop() {
  
  uint8_t i;
  if (server.hasClient()){
      if (!serverClient || !serverClient.connected()){
        if(serverClient) serverClient.stop();
        serverClient = server.available();
      }
    }
    //no free spot
    WiFiClient serverClient = server.available();
    serverClient.stop();
    if (serverClient && serverClient.connected()){
      if(serverClient.available()){
        while(serverClient.available()) Serial.write(serverClient.read());
        //you can reply to the client here
        serverClient.write(1);
      }
    }
  /*
  if(Serial.available()){
    size_t len = Serial.available();
    uint8_t sbuf[len];
    Serial.readBytes(sbuf, len);
    //bello is a broadcast to all clients
    for(i = 0; i < MAX_SRV_CLIENTS; i++){
      if (serverClients[i] && serverClients[i].connected()){
        serverClients[i].write(sbuf, len);
        delay(1);
      }
    }
  }*/
}
