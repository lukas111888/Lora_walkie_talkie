/*
    Video: https://www.youtube.com/watch?v=oCMOYS71NIU
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
    Ported to Arduino ESP32 by Evandro Copercini
    updated by chegewara

   Create a BLE server that, once we receive a connection, will send periodic notifications.
   The service advertises itself as: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
   And has a characteristic of: beb5483e-36e1-4688-b7f5-ea07361b26a8

   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create a BLE Service
   3. Create a BLE Characteristic on the Service
   4. Create a BLE Descriptor on the characteristic
   5. Start the service.
   6. Start advertising.

   A connect hander associated with the server starts a background task that performs notification
   every couple of seconds.
*/


#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <string> 

// DEVICE_INFORMATION 
#define SERVICE_UUID        "180A"
#define UUID16_CHR_SERVICE_CHANGED "2A05"
#define UUID16_CHR_TEMPERATURE_MEASUREMENT  "2A1C"


//Declare variables
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* temperature = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

//Initialize variables
uint32_t value = 0;
uint32_t hrm = 0;
uint32_t statement=0;
uint32_t last_statement=0;
int threshold = 20; //The bigger the threshold, the more sensible is the touch
bool Button = false;
bool last_Button = false;
bool touchActive = false;
bool lastTouchActive = false;
bool testingLower = true;



void gotPush(){  
 Button = !digitalRead(T1);
 if (Button ==1){ 
  String message_lora="GPS:47.98246466,-122.15719442";
  Serial.println(message_lora);
  pCharacteristic->setValue(message_lora.c_str());
  pCharacteristic->notify();      
 }else{
  String message_lora="DoNe";
  Serial.println(message_lora);
  pCharacteristic->setValue(message_lora.c_str());
  pCharacteristic->notify();   
 }
}


//Server Callback
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

void setup() {
  Serial.begin(115200);
  //Initialize switch interrupts 
  attachInterrupt(T1, gotPush, CHANGE);
  
  // Create the BLE Device
  BLEDevice::init("UW Thermo-Clicker");
  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);
  // Create a switch Characteristic
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
  BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection to notify...");  
}

void loop() {
    // notify changed value
    if (deviceConnected) {       
        
        String getdata = temperature->getValue().c_str();  
        Serial.println(getdata); 
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
    delay(1000);   
}
