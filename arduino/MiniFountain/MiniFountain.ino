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
#include <DHT.h>
#include <DFRobotDFPlayerMini.h>

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
#define ledPin 5
#define ledNum 2
Adafruit_NeoPixel leds(ledNum, ledPin, NEO_GRB + NEO_KHZ800);

/** DHT11 Sensor **/
#define dhtPin 4
DHT dht(dhtPin, DHT11);
float humi;

/** Sound **/
#define soundPin A0
#define DF_RX 9
#define DF_TX 10
SoftwareSerial dfSerial(DF_RX, DF_TX);
DFRobotDFPlayerMini dfPlayer;
boolean isEnabledDFPlayer = false;
boolean isActivedPlayMusic = false;

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

  // Begin DHT11
  dht.begin();
  humi = dht.readHumidity();
  Serial.println("Humidity : " + String(humi)); 
  delay(100);
  if(humi < 65) {
    int power = map(humi, 20, 90, 255, 0);
    Serial.println("Power (Humidity Mode): " + String(power)); 
    powerSet(power);
  }

  // Begin DFPlayer Mini
  if(!dfPlayer.begin(dfSerial)) {
    Serial.println("Failed to Load MP3 Module.");
    isEnabledDFPlayer = false;
  }
  else {
    isEnabledDFPlayer = true;
    dfPlayer.volume(20);
  }
}

void loop() {
  // put your main code here, to run repeatedly:
  if(BTSerial.available()) {
    String btData = BTSerial.readString();
    Serial.println("BT : " + btData);

    String convtData = "";
    if(btData.indexOf("color") != -1) {
      convtData = btData.substring(8, 14);
      Serial.println("Color : " + convtData);
      changeRGB(convtData);
    }
    else if(btData.indexOf("power") != -1) {
      convtData = btData.substring(6, 9);
      int power = convtData.toInt();
      powerSet(power);
      Serial.println("Power : " + convtData);
      Serial.println("Power (Map) : " + String(power)); 
    }
    else if(btData.indexOf("bluetooth;connect!") != -1) {
      isCheckSend = false;
      isConnected =  true;
      Serial.println("Bluetooth Connection Check Complete.");
    }
    else if(btData.indexOf("bluetooth;connected") != -1) {
      isConnected = true;
      powerSet(0);
      Serial.println("Bluetooth Connected!");
    }
    else if(btData.indexOf("bluetooth;disconnected") != -1) {
      isConnected = false;
      Serial.println("Bluetooth Disconnected");
    }
    else if(btData.indexOf("playaudio;") != 1) {
      convtData = btData.substring(6, 7);
      int songNum = convtData.toInt();

      if(isEnabledDFPlayer == false) {
        BTSerial.write("playaudio;failedbegin\r\n");
        isActivedPlayMusic = false;
      }
      else {
        isActivedPlayMusic = true;
        dfPlayer.play(songNum);
      }
    }
  }

  time = millis();
  if(connectedCheckTime == time - preTime) {
    if(isActivedPlayMusic == false) {
      if(isConnected == true) {
        if(isCheckSend == false) {
          preTime = time;
          BTSerial.write("bluetooth;connect?\r\n");
          isCheckSend = true;
          Serial.println("Check Bluetooth Connection...");
        }
        else {
          isConnected = false;
          Serial.println("Failed to Bluetooth Communication.");
        }
      }
      else {
        humi = dht.readHumidity();
        if(humi < 50) {
          int power = map(humi, 20, 90, 255, 0);
          Serial.println("Humidity : " + String(humi)); 
          Serial.println("Power (Humidity Mode): " + String(power)); 
          powerSet(power);
        }
      }
    }
  }

  if(isActivedPlayMusic == true) {
    int power = map(analogRead(soundPin), 0, 1023, 0, 255);
    powerSet(power);
    Serial.println("Power (Music Mode): " + String(power)); 
  }
  delay(100);
}

void changeRGB(String hexString) {
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
    leds.setPixelColor(i, leds.Color((int) r, (int) g, (int) b));
  }
  leds.show();
}

void powerSet(int power) {
    digitalWrite(motorAp, HIGH);
    digitalWrite(motorAm, LOW);
    analogWrite(motorAp, power);

    digitalWrite(motorBp, HIGH);
    digitalWrite(motorBm, LOW);
    analogWrite(motorBp, power);
}
