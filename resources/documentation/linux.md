### Linux Serial Ports
Access to to USB, HID-based programmers, such as the Atmel-ICE, is limited to certain users and groups in Linux, which may prevent you from selecting and using these programmers.  This is normally fixed by adding special "rules" to a file in the "`/etc/udev/rules.d/`" directory.  However, in my testing using Mint LInux 19 (running in emulation on Parallels), I have been unable, so far, to create a rule file that works.  If you know how to fix this, please post a comment in the "[Issues](https://github.com/wholder/MegaTinyIDE/issues)" page.

However, until I can sort this out, you should be able to run MegaTinyIDE as root, like this:

   `$ sudo java -jar MegaTinyIDE.jar`
   
and providing your root password, when asked.