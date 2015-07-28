/*
  by Jarkko Saltiola (jasalt)
  Utilizing examples
  - Communication > SerialCallResponse
  - Servo > Sweep
*/

#include <Servo.h>

Servo myservo;  // create servo object to control a servo
                // a maximum of eight servo objects can be created

int pos = 0;    // variable to store the servo position
int inByte = 0; // incoming bytes

void setup() {
  Serial.begin(9600);
  myservo.attach(9);  // attaches the servo on pin 9 to the servo object

  myservo.write(90); // start from servo center position
  establishContact(); // send a byte to establish contact until receiver responds
}

void servoTo(int deg) {
  /* Move servo to specified degree position */
  myservo.write(deg);
}

void sweep(int ms) {
  /* Goes from 0 degrees to 180 degrees in steps of 1 degree with speed defined
     by timeout in ms. */
  for(pos = 0; pos < 180; pos += 1) {
    servoTo(pos);
    delay(ms);
  }
  for(pos = 180; pos>=1; pos-=1) {
    servoTo(pos);
    delay(ms);
  }
}

void establishContact() {
  while (Serial.available() <= 0) {
    Serial.print('A');
    delay(300);
  }
}

int readControlValue(){
  if (Serial.available() > 0) {
    inByte = Serial.read();
    //Serial.write(inByte);
    Serial.write(inByte);
    return inByte;
  }
}

void loop() {
  readControlValue();
  servoTo(inByte);
  //sweep(50);
  
  delay(50);


}

