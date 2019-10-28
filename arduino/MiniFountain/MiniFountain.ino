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

#define BT_RX 2
#define BT_TX 3

SoftwareSerial BTSerial(BT_RX, BT_TX);

void setup() {
  // Serial (Debug)
  Serial.begin(9600);

  // Bluetooth Software Serial
  BTSerial.begin(19200);
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
      Serial.println("Power : " + convtData);
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
  //color(r, g, b)
}
