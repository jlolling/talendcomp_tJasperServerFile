# Talend Components to handle files in the JasperServer repository

These component provide the capabilties to handle files on the JasperServer.
They use now the rest-v2 interface and therefore compatible with the current releases of the JaspeServer.
You can do all typical file operations with them.
A typical use case is to send offline generated report result files on a JasperServer.

There are following components
tJasperServer - provides the connection to the JasperServer to share it over multiple components
tJasperServerFilePut - Put a file in the repository
tJasperServerFileGet - Get a file from the repository
tJasperServerFileDelete - Delete a file from the repository
tJasperServerFileCopy - Copy/move a file within the repository
tJasperServerFileList - List files in the repository
tJasperServerClose - Close the explicit connection created by tJasperServer component

![Roundtrip job](https://github.com/jlolling/talendcomp_tJasperServerFile/blob/master/doc/tJasperServer_scenario_roundtrip_new.png)
