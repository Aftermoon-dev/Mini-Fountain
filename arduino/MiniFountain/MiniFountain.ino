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
#define maxMotorValue 200
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
#define ledNum 4
Adafruit_NeoPixel leds(ledNum, ledPin, NEO_GRB + NEO_KHZ800);
boolean isRainbowEnable = false;

/** DHT11 Sensor **/
#define dhtPin 4
#define minHumi 40
DHT dht(dhtPin, DHT11);
float humi;

/** Sound **/
#define soundPin A0
#define DF_RX 8
#define DF_TX 9
#define DF_BUSY 10
#define DF_VOLUME 15
SoftwareSerial dfSerial(DF_RX, DF_TX);
DFRobotDFPlayerMini dfPlayer;
boolean isEnabledDFPlayer = false;
boolean isActivedPlayMusic = false;

void setup() {
  // Begin Serial (Debug)
  Serial.begin(115200);
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
  if(humi < minHumi) {
    int power = map(humi, 20, 90, maxMotorValue, 0);
    Serial.println("Power (Humidity Mode): " + String(power)); 
    powerSet(power);
  }

  // Begin DFPlayer Mini
  pinMode(DF_BUSY, INPUT);
  dfSerial.begin(9600);
  
  if(!dfPlayer.begin(dfSerial)) {
    Serial.println("Failed to Load MP3 Module");
    isEnabledDFPlayer = false;
  }
  else {
    isEnabledDFPlayer = true;
    dfPlayer.volume(DF_VOLUME);
    Serial.println("Success to Load MP3 Module"); 
  }
}

void loop() {
  int dfPlayer_State = digitalRead(DF_BUSY);
  if(dfPlayer_State == HIGH) {
    isActivedPlayMusic = false;
  }
  
  BTSerial.listen();
  String btData = "";
  String convtData = "";
  
  while(BTSerial.available()) {
    char getBT = (char) BTSerial.read();
    btData += getBT;
    delay(5);
  }  
  
  if(btData.indexOf("color") != -1) {
    convtData = btData.substring(8, 14);    
    Serial.println("Color : " + convtData);
    changeRGB("#" + convtData);
  }
  else if(btData.indexOf("pw") != -1) {
    convtData = btData.substring(3, 6);
    int power = convtData.toInt();
    powerSet(power);
    Serial.println("Power : " + convtData);
    Serial.println("Power (Map) : " + String(power)); 
  }
  else if(btData.indexOf("bluetooth;connect!") != -1) {
    isCheckSend = false;
    isConnected = true;
    Serial.println("Bluetooth Connection Check Complete.");
  }
  else if(btData.indexOf("bluetooth;connected") != -1) {
    isConnected = true;
    powerSet(0);
    isActivedPlayMusic = false;
    dfPlayer.pause();
    Serial.println("Bluetooth Connected!");
  }
  else if(btData.indexOf("bluetooth;disconnected") != -1) {
    isConnected = false;
    isActivedPlayMusic = false;
    leds.clear();
    leds.show();
    powerSet(0);
    dfPlayer.pause();
    Serial.println("Bluetooth Disconnected");
  }
  else if(btData.indexOf("playaudio;play") != -1) {
    convtData = btData.substring(14, 15);
    int songNum = convtData.toInt();
    Serial.println("Song Number : " + convtData);

    if(isEnabledDFPlayer == false) {
      BTSerial.write("playaudio;failedbegin\r\n");
      isActivedPlayMusic = false;
    }
    else {
      isActivedPlayMusic = true;
      dfPlayer.play(songNum);
      BTSerial.write("audio;start\r\n");
    }
  }
  else if(btData.indexOf("playaudio;stop") != -1) {
   if(isActivedPlayMusic == true) {
     isActivedPlayMusic = false;
     leds.clear();
     leds.show();
     powerSet(0);
     dfPlayer.pause();
     BTSerial.write("audio;stop\r\n");
   }
  }
  else if(btData.indexOf("rainbow;start") != -1) {
    BTSerial.write("led;rainbow_start\r\n");
    isRainbowEnable = true;
  }
  else if(btData.indexOf("rainbow;stop") != -1) {
    BTSerial.write("led;rainbow_stop\r\n");
    isRainbowEnable = false;
    leds.clear();
    leds.show();
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
        if(humi < minHumi) {
          int power = map(humi, 20, 90, maxMotorValue, 0);
          Serial.println("Humidity : " + String(humi)); 
          Serial.println("Power (Humidity Mode): " + String(power)); 
          powerSet(power);
        }
      }
    }
  }

  if(isActivedPlayMusic == true) {
    int power = map(analogRead(soundPin), 0, 1023, 0, maxMotorValue);
    powerSet(power);
    Serial.println("Power (Music Mode): " + String(power)); 
    rainbow(2);
  }

  if(isRainbowEnable == true) {
    rainbow(3);
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
    delay(100);
    
    digitalWrite(motorBp, HIGH);
    digitalWrite(motorBm, LOW);
    analogWrite(motorBp, power);
    delay(100);
}

void rainbow(int wait) {
  for(long firstPixelHue = 0; firstPixelHue < 5*65536; firstPixelHue += 256) {
    for(int i=0; i<leds.numPixels(); i++) {
      int pixelHue = firstPixelHue + (i * 65536L / leds.numPixels());
      leds.setPixelColor(i, leds.gamma32(leds.ColorHSV(pixelHue)));
    }
    leds.show();
    delay(wait);
  }
}
