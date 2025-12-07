# DriveWire IO Error Diagnostics

## Error Codes
- `DWOK` (0x00) = Success
- `DWERROR_NOTREADY` (0xF6) = Drive not ready (no disk or invalid drive)
- `DWERROR_READ` (0xF4) = Read error (invalid sector, bad image format, seek past end)
- `DWERROR_WRITE` (0xF5) = Write error
- `DWERROR_WP` (0xF2) = Write protected
- `DWERROR_CRC` (0xF3) = Checksum error

## Common Causes of "?IO error" when accessing Drive 0

### 1. **No Disk Inserted**
- **Symptom**: `DWERROR_NOTREADY` (0xF6)
- **Solution**: Insert a disk image into drive 0 using the UI
- **Check**: Look at the disk table - drive 0 should show a filename

### 2. **Disk Image File Missing or Unreadable**
- **Symptom**: `DWERROR_NOTREADY` or `DWERROR_READ`
- **Solution**: 
  - Verify the disk image file exists at the path shown in the UI
  - Check file permissions
  - Try re-inserting the disk

### 3. **Invalid Disk Image Format**
- **Symptom**: `DWERROR_READ` (0xF4)
- **Solution**: 
  - Ensure the disk image is a valid DSK format
  - Try a known-good disk image
  - Check server logs for "DWImageFormatException"

### 4. **Network Connection Issues (TCP)**
- **Symptom**: Timeouts, connection errors
- **Solution**:
  - Verify the server is running and accessible
  - Check firewall settings
  - Verify IP address and port are correct
  - Check server logs for connection errors

### 5. **Instance Mismatch**
- **Symptom**: Connected but drives don't work
- **Solution**:
  - Verify you're connected to the correct instance
  - Check the instance number in the server config
  - Use "Choose instance" menu to switch instances

### 6. **Server Not Ready**
- **Symptom**: Errors immediately after connection
- **Solution**:
  - Wait a few seconds after connecting for server to initialize
  - Check server logs for initialization errors

## Diagnostic Steps

1. **Check UI Status**:
   - Is the status "Connected"?
   - Does drive 0 show a filename in the disk table?
   - Are there any error messages in the Log tab?

2. **Check Server Logs**:
   - Look for "DoOP_READ" messages
   - Check for exceptions like:
     - `DWDriveNotLoadedException` = No disk in drive
     - `DWDriveNotValidException` = Invalid drive number
     - `DWInvalidSectorException` = Bad sector number
     - `DWSeekPastEndOfDeviceException` = Sector beyond disk size
     - `DWImageFormatException` = Invalid disk image format

3. **Test with Known Good Disk**:
   - Try inserting a disk image that works on another system
   - Verify the disk image file is not corrupted

4. **Check Network Connection (if using TCP)**:
   - Ping the server IP
   - Verify port is open (telnet to IP:port)
   - Check server firewall rules

5. **Verify DriveWire Protocol**:
   - Ensure VCC CoCo is configured for DriveWire protocol
   - Check baud rate settings match server
   - Verify TCP port matches server port

## Server-Side Debugging

Enable detailed logging in `config.xml`:
```xml
<LogOpCode>true</LogOpCode>
<LogToConsole>true</LogToConsole>
```

This will show:
- Every disk operation (READ/WRITE)
- Drive number and LSN (Logical Sector Number)
- Error codes returned to CoCo

## VCC CoCo Configuration

**IMPORTANT**: VCC CoCo connects to a **virtual serial port TCP listener**, NOT the UI port (6800).

### Setup Steps:

1. **Configure a virtual serial port to listen on TCP**:
   - In the DriveWire UI command area, run:
     ```
     dw port tcp 0 listen 6551
     ```
   - This sets virtual serial port 0 to listen on TCP port 6551
   - You can use any available port (e.g., 6551, 6552, etc.)
   - **IMPORTANT**: Use a different port than 6800 (which is the UI command port)

2. **Configure VCC CoCo**:
   - **DriveWire Type**: TCP/IP (not serial)
   - **Server IP**: 127.0.0.1 (or the server's IP address)
   - **Server Port**: 6551 (or whatever port you configured in step 1)
   - **Instance**: Match the instance number (usually 0 or 2)

3. **Verify the TCP listener is active**:
   - Check the server logs for "listening on port 6551"
   - Or use the command: `dw port show` or `dw net show`

### Troubleshooting VCC CoCo Connection:

- **"Connection refused"**: The TCP listener isn't running. Run `dw port tcp 0 listen 6551`
- **"Cannot connect"**: Check firewall settings, ensure the port isn't blocked
- **"IO error"**: See disk-related troubleshooting above
- **Wrong instance**: Ensure VCC CoCo instance matches DriveWire instance (check UI status bar)

