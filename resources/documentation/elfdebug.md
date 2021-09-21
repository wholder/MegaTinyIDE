### Debugging ELF files

MegaTinyIDE can also be used to debug externally compiled and built ELF files when invoked directly from the command line.  In most cases, MegaTinyIDE can determine the target device, such as "attiny3217", from the ELF file.  So, you can invoked it like this:

  `MegaTinyIDE <path to ELF file>`

However, if MegaTinyIDE fails to detect the target device type, it can also be invoked like this:

  `MegaTinyIDE <target device, such as "attiny3217"> <path to ELF file>`

Either command should load the ELF file and bring up the Listing tab with the debugging pane displayed at the top of the page and the code displayed below.  Then, see the section on [Using the Debugger](debugging.md) for further details.