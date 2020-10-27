### USB Devices on Linux
Access to to USB, HID-based programmers, such as the Atmel-ICE, is limited to certain users and groups in Linux, which may prevent you from selecting and using these programmers.  This is normally fixed by adding special "rules" to a file in the "`/etc/udev/rules.d/`" directory.  You can create tne needed rules file by running the following command line in the Linux terminal program:

`echo 'KERNEL=="hidraw*", ATTRS{idVendor}=="03eb", ATTRS{idProduct}=="*", MODE:="0666", GROUP="plugdev", TAG+="uaccess", TAG+="udev-acl"' | sudo tee /etc/udev/rules.d/100-atmel.rules`

This command should be entered as a single line, followed by pressing enter, after which you'll be prompted to enter your root password.  Then, you'll need to restart Linux for the new rules file to take effect.  After that, you should be able to run MegaTinyIDE as you would any other Java-based program on a Linux system.  Of course, this assumes you have installed Java version 8, or later and have configured the `MegaTinyIDE.jar` file to be executable.

Note: this command gives access permission to all products made by Microchip (vendor id `03eb`.)  To narrow the permission to a single device, change the '*' in `ATTRS{idProduct}=="*"` to one of the following product ids:

  - `2141` for Atmel-ICE
  - `2177` for PICkit4
  - `2145` for ATTiny416-Xplained-Nano or Mini
  - `2175` for ATTiny3217-Curiosity-Nano

If the above fix does not work, you should be able to run MegaTinyIDE as root from the command line, like this:

   `sudo java -jar path-as-needed/MegaTinyIDE.jar`
   
and providing your root password, when asked.  However, if you use this approach, you'll need to use this command every time you want to run MegaTinyIDE.