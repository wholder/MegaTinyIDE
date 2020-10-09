
#pragma chip attiny412

#include <avr/io.h>
#include <stdbool.h>
#include "add.h"

#define BIT_SET(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define BIT_CLR(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

//#define PASTE(A, B) (A ## B)

#define BIT_IN(P, B)  ((P & (1 << B)) != 0)

int main () {
	PORTA.DIR = 0b01001000;         		// sets PA3 and PA6 as an output
	PORTA.DIR = PIN2_bm | PIN3_bm;  		// sets PA3 and PA6 as an output

	PORTA.DIRSET = PIN1_bm; 						// use PA4 as an output
	PORTA.DIRCLR = PIN1_bm; 						// use PA4 as an input

	PORTA.OUT |= PIN1_bm; 							// write PB4 high (not atomic)
	PORTA.OUT &= ~PIN1_bm; 							// write PB4 low (not atomic)

	PORTA.OUTSET = PIN1_bm; 						// turn PA4 output on (atomic)
	PORTA.OUTCLR = PIN1_bm; 						// turn PA4 output off (atomic)

	PORTA.OUTTGL = PIN1_bm; 						// toggle PA4 output

	PORTA.PIN3CTRL = PORT_PULLUPEN_bm; 	// enable the internal pullup resistor on PA6
	PORTA.PIN3CTRL = 0; 								// disable the internal pullup resistor on PA6

	TCD0.INTFLAGS = 0x00;

	int sum = add(5, 4);

	// More efficient to use VPORTA in combination with "sbi" and "cbi"

	VPORTA.DIR = 0x55;									// Set PORTA direction reg to 0x55
	VPORTA.OUT = 0x32;									// Set PORTA output reg to 0x32

  asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(VPORTA.OUT)), "I" (2));
	asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(VPORTA.OUT)), "I" (2));

	BIT_SET(VPORTA.OUT, 4);							// Set bit 4 in PORTA output reg
	BIT_CLR(VPORTA.OUT, 4);							// Clear bit 4 in PORTA  output reg

	BIT_SET(VPORTA.DIR, 4);							// Set bit 4 in PORTA to output
	BIT_CLR(VPORTA.DIR, 4);							// Set bit 4 in PORTA to input

	BIT_SET(VPORTA.IN, 4);							// Toggle bit 4 in PORTA
	BIT_CLR(VPORTA.IN, 4);							// Toggle bit 4 in PORTA

	bool tmp = BIT_IN(VPORTA.IN, 1);

	//BIT_SET(PASTE(VPORTA, .IN), 4);			// Toggle bit 4 in PORTA
	
}