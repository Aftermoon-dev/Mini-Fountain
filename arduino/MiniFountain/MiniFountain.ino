/*
  MIT License
  
  Copyright (C) 2019 Aftermoon
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

#include <SoftwareSerial.h>
#include <Adafruit_NeoPixel.h>

/** Motor Setting **/
#define motorAp 6
#define motorAm 7
#define motorBp 11
#define motorBm 12

/** Bluetooth Setting **/
#define BT_RX 2
#define BT_TX 3
#define connectedCheckTime 60000
SoftwareSerial BTSerial(BT_RX, BT_TX);
unsigned long time;
unsigned long preTime;
boolean isCheckSend = false;
boolean isConnected = false;

/** LED (NeoPixel) Setting **/
#define ledPin 9
#define ledNum 4
Adafruit_NeoPixel leds(ledNum, ledPin, NEO_GRBW + NEO_KHZ800);

void setup() {
  // Begin Serial (Debug)
  Serial.begin(9600);
  Serial.println("Mini Fountain System Online");
  Serial.println("Made by Team Midnight");
  
  // Begin Bluetooth
  BTSerial.begin(19200);

  // Begin LED
  pinMode(ledPin, OUTPUT);
  leds.begin();
  leds.show();
}

void loop() {
  // put your main code here, to run repeatedly:
  if(BTSerial.available()) {
    String btData = BTSerial.readString();

    String convtData = "";
    if(btData.indexOf("color") != -1) {
      convtData = btData.substring(8, 14);
      Serial.println("Color : " + convtData);
      RGBChange(convtData);
    }
    else if(btData.indexOf("power") != -1) {
      convtData = btData.substring(6, 9);
      int power = map(convtData.toInt(), 0, 100, 0, 255);
      
      digitalWrite(motorAp, HIGH);
      digitalWrite(motorAm, LOW);
      analogWrite(motorAp, power);

      digitalWrite(motorBp, HIGH);
      digitalWrite(motorBm, LOW);
      analogWrite(motorBp, power);
      
      Serial.println("Power : " + convtData);
      Serial.println("Power (Map) : " + String(power)); 
    }
    else if(btData.indexOf("bluetooth;connect") != -1) {
      isCheckSend = false;
      Serial.println("Bluetooth Connection Check Complete.");
    }
    else if(btData.indexOf("bluetooth;connected") != -1) {
      isConnected = true;
      Serial.println("Bluetooth Connected!");
    }
    else if(btData.indexOf("bluetooth;disconnected") != -1) {
      isConnected = false;
      Serial.println("Bluetooth Disconnected.");
    }
  }

  time = millis();
  if(connectedCheckTime == time - preTime) {
    if(isCheckSend == false) {
      if(isConnected == true) {
        preTime = time;
        BTSerial.write("bluetooth;connect?");
        isCheckSend = true;
        Serial.println("Check Bluetooth Connection...");
      }
    }
    else {
      isConnected = false;
      Serial.println("Failed to Bluetooth Communication.");
    }
  }
}

void RGBChange(String hexString) {
  // Convert Hex Color Code to RGB
  // https://stackoverflow.com/questions/23576827/arduino-convert-a-string-hex-ffffff-into-3-int
  long long number = strtol( &hexString[1], NULL, 16);
  
  // Split them up into r, g, b values
  long r = number >> 16;
  long g = number >> 8 & 0xFF;
  long b = number & 0xFF;

  String rgb = String(r) + "," + String(g) + "," + String(b);
  Serial.println("RGB Color : " + rgb);
  
  for(int i = 0; i < ledNum; i++) {
    leds.setPixelColor(i, (int) r, (int) g, (int) b);
  }
  
  leds.show();
}
