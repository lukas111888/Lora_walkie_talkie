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
String localAddress = "AA";
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint32_t value = 0;
String message_phone;
String message_lora;
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

void LoRaData() {
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->drawString(0 , 15 , "Received " + packSize + " bytes");
  Heltec.display->drawStringMaxWidth(0 , 26 , 128, packet);
  Heltec.display->drawString(0, 0, rssi);
  Heltec.display->display();
}
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
  BLEDevice::init("ESP32");

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
  //BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection to notify...");
  message_phone_old = " ";
}
/*
bool check(){  
  for(int i=0; i<1;i++){
    LoRa.beginPacket();
    LoRa.setTxPower(14,RF_PACONFIG_PASELECT_PABOOST);
    LoRa.print(localAddress);
    LoRa.endPacket();
    Serial.println("here1");
    //delay(2000);  
    int packetSize = LoRa.parsePacket();
    if (packetSize) {
       String Add= readfromlora(packetSize);
       Serial.println("here2");
       if (Add == localAddress){
        return true;
       }
       else{
        return false;
       }
    } 
  }
  Serial.println("here3");
  return false; 
}*/


void loop() {
  deviceConnected = true;
  Serial.println(value);
  if (deviceConnected) {      
    //Get message from lora
    int packetSize = LoRa.parsePacket();
    if (packetSize) {
      message_lora = readfromlora(packetSize);
      Serial.println("message_lora:"+message_lora);
      if (message_lora.substring(0,3) == "GPS" or message_lora.substring(0,3) =="TXT"){
        // Send message to phone
        // uncommend for BLE
        //pCharacteristic->setValue(message_lora.c_str());
        //pCharacteristic->notify();
        Serial.println(message_lora);
        message_lora_old = message_lora;
      }else if(message_lora_old != message_lora){
        LoRa.beginPacket();
        LoRa.setTxPower(14,RF_PACONFIG_PASELECT_PABOOST);
        LoRa.print(message_lora);
        LoRa.endPacket();     
      }
    }else{
      // Get message from phone 
      // commend for BLE
      message_phone = "TXT"+localAddress+":"+String(value);
      // uncommend for BLE      
      //message_phone = temperature->getValue().c_str(); 
      if (message_phone !=message_phone_old){
      // Send message to lora
        bool checking = true;//check();
        if (checking){
          LoRa.beginPacket();
          LoRa.setTxPower(14,RF_PACONFIG_PASELECT_PABOOST);
          LoRa.print(message_phone);
          LoRa.endPacket();
          Serial.println(message_phone);
          message_phone_old = message_phone;          
        }
      }    
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
  }
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->drawString(0, 0, "Own: "+message_phone_old);
  Heltec.display->drawString(0, 10, "Other: "+message_lora_old);
  Heltec.display->display();
  delay(300);
}
