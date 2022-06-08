#include "heltec.h"
#include "images.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#define BAND    915E6  //you can set band here directly,e.g. 868E6,915E6
#define SERVICE_UUID        "180A"
#define UUID16_CHR_SERVICE_CHANGED "2A05"
#define UUID16_CHR_TEMPERATURE_MEASUREMENT  "2A1C"
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* temperature = NULL;
String localAddress ;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint32_t value = 0;
String message_phone="";
String message_lora="";
String message_phone_old="";
String message_lora_old="";
String rssi = "RSSI --";
String packSize = "--";
String packet ;
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};
void logo() {
  Heltec.display->clear();
  Heltec.display->drawXbm(0, 5, logo_width, logo_height, logo_bits);
  Heltec.display->display();
}
/*
void LoRaData() {
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->drawString(0 , 15 , "Received " + packSize + " bytes");
  Heltec.display->drawStringMaxWidth(0 , 26 , 128, packet);
  Heltec.display->drawString(0, 0, rssi);
  Heltec.display->display();
}
*/
void gotPush(){  
 value ++;
}
String readfromlora(int packetSize) {
  packet = "";
  packSize = String(packetSize, DEC);
  for (int i = 0; i < packetSize; i++) {
    packet += (char) LoRa.read();
  }
  rssi = String(LoRa.packetRssi()) ;
  //LoRaData();
  return packet;
}
void sendtolora(String msg){
  LoRa.beginPacket();
  LoRa.setTxPower(14,RF_PACONFIG_PASELECT_PABOOST);
  LoRa.print(msg);
  LoRa.endPacket(); 
}

void GenTxt(){
  Serial.println("?");
  int generated=0;
  while (generated<6){
     int randomValue = random(0, 26);
     char letter = randomValue + 'a';
     localAddress += letter;
     generated ++;
     Serial.println(localAddress);
  }
}

void setup() {
  //WIFI Kit series V1 not support Vext control
  Heltec.begin(true /*DisplayEnable Enable*/, true /*Heltec.Heltec.Heltec.LoRa Disable*/, true /*Serial Enable*/, true /*PABOOST Enable*/, BAND /*long BAND*/);

  Heltec.display->init();
  Heltec.display->flipScreenVertically();
  Heltec.display->setFont(ArialMT_Plain_10);
  logo();
  attachInterrupt(T1, gotPush, FALLING);
  delay(1500);
  Heltec.display->clear();
  Heltec.display->drawString(0, 0, "Heltec.LoRa Initial success!");
  Heltec.display->drawString(0, 10, "Wait for incoming data...");
  Heltec.display->display();
  
  delay(1000);
  //LoRa.onReceive(cbk);
  LoRa.receive();
  // Create the BLE Device
  BLEDevice::init("UW LoRa B");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      UUID16_CHR_SERVICE_CHANGED,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );
  // Create a temperature Characteristic
  temperature = pService->createCharacteristic(
                      UUID16_CHR_TEMPERATURE_MEASUREMENT,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );  


  // Start the service
  pService->start();
  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  // uncommend for BLE  
  BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection to notify...");
  //message_phone_old = "";
  GenTxt();
}

void loop() {  
  // commend for BLE
  //deviceConnected = true;
  if (deviceConnected) {     
    //Get message from lora
    int packetSize = LoRa.parsePacket();
    if (packetSize) {
      message_lora = readfromlora(packetSize);
      //Check message get back successful 
      if(message_lora==message_phone){        
          message_phone_old = message_phone;
          // uncommend for BLE
          pCharacteristic->setValue("DoNe");
          pCharacteristic->notify();          
      }else if (message_lora.substring(6,7) == "G" or message_lora.substring(6,7) =="T"){
        // Send message to phone
        // uncommend for BLE
        String temp = message_lora.substring(6);
        pCharacteristic->setValue(temp.c_str());
        pCharacteristic->notify();
        //Serial.println(message_lora);
        message_lora_old = message_lora;
        //Call back to check message received successful
        sendtolora(message_lora); 
        //Check message updata successful        
      }else if(message_lora_old != message_lora){
        sendtolora(message_lora); 
      }
      Serial.println("get");
      Serial.print("own value"+message_phone+"  ");
      Serial.println("anther"+message_lora);  
    }else{
      // Get message from phone 
      // commend for BLE
      //message_phone = "TXT:"+localAddress+":"+String(value);
      // uncommend for BLE   
      //Prevent both parties send the same content   
      message_phone = localAddress+temperature->getValue().c_str(); 
      if (message_phone !=message_phone_old and message_phone!=""){
        // Send message to lora
        // random send rate prevent conflict       
        int randNumber = random(100);
        if (randNumber%5==0){
          sendtolora(message_phone);                 
          Serial.println("send");
          Serial.print("own value"+message_phone+"  ");
          Serial.println("anther"+message_lora);                       
        }
      }/*else if(message_phone==message_phone_old and message_phone!=""){
        if(pCharacteristic->getValue().c_str() != "DoNe"){
          pCharacteristic->setValue("DoNe");
           Serial.println(pCharacteristic->getValue()
          pCharacteristic->notify(); 
        }                   
      }
      */ 
    }
  }
  
  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("start advertising");
    oldDeviceConnected = deviceConnected;
  }
  // connecting
  if (deviceConnected && !oldDeviceConnected) {
    // do stuff here on connecting
    oldDeviceConnected = deviceConnected;
    Serial.println("DeviceConnected");
  }
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_16);
  Heltec.display->drawString(0, 0, " Own: "+message_phone_old.substring(6));
  Heltec.display->drawString(0, 16, " Other: "+message_lora_old.substring(6));  
  Heltec.display->drawString(0, 32, localAddress );
  Heltec.display->drawString(0, 48, String(deviceConnected));
  Heltec.display->display();
  delay(100);
}
