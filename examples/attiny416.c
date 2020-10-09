
#pragma chip attiny416

#include <avr/io.h>
#include <stdbool.h>

//										 Reset   LED BTN
// ATTiny416 Xpl Nano  pin 19  PB5 PB4

#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

#define BIT_SET(P, B) (((P) & (1 << (B))) != 0)
#define BIT_CLR(P, B) (((P) & (1 << (B))) == 0)

//            +----------+
//				Vdd | 1   	20 | Gnd					PORTA |7|6|5|4|3|2|1|0|
//    		PA4 | 2   	19 | PA3					PORTB |-|-|5|4|3|2|1|0|
//    		PA5 | 3   	18 | PA2					PORTA |-|-|-|-|3|2|1|0|
//    		PA6 | 4   	17 | PA1
//    		PA7 | 5   	16 | PA0
//   LED	PB5 | 6   	15 | PC3
//   BTN	PB4 | 9   	14 | PC2
//    		PB3 | 8   	13 | PC1
//    		PB2 | 9   	12 | PC0
//    		PB1 | 10  	11 | PB0
//            +----------+

int main () {
	SET_BIT(VPORTB.DIR, 5);							// Set bit 5 in PORTA (pin 10) to output
  PORTA.PIN4CTRL |= PORT_PULLUPEN_bm; // use the internal pullup resistor on PA4
	while (true) {
		SET_BIT(VPORTB.OUT, 5);						// Set bit 5 in PORTA (pin 10) HIGH
		CLR_BIT(VPORTB.OUT, 5);						// Clr bit 5 in PORTA (pin 10) LOW
		if (BIT_CLR(VPORTB.IN, 4)) {			// Check if BTN is pressed (PB4 goes LOW)
			asm volatile("BREAK");
		}
	}
}