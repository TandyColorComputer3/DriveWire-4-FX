# DriveWire TCP Device Connection Handling Rules

## Critical Requirements for DWTCPDevice

### ServerSocket.accept() Timeout

**REQUIRED**: `ServerSocket.accept()` MUST have a timeout to prevent blocking the protocol handler loop indefinitely.

**Implementation:**
```java
// Set timeout before accept()
srvr.setSoTimeout(5000); // 5 seconds
skt = srvr.accept();
// Reset timeout after accepting
srvr.setSoTimeout(0);
```

**Why:** Without a timeout, `accept()` blocks indefinitely, preventing the protocol handler loop from:
- Initializing properly
- Processing other operations
- Being ready when clients try to connect

**Timeout Value:** 5 seconds is optimal:
- Long enough for normal client connections
- Short enough to keep the protocol handler loop responsive
- Allows multiple retry opportunities for clients

### SocketTimeoutException Handling

**REQUIRED**: `SocketTimeoutException` from `accept()` must be caught and handled gracefully.

**Implementation:**
```java
catch (java.net.SocketTimeoutException e)
{
    // Timeout is expected - no client connected yet
    // Don't log as error - only debug level
    logger.debug("No client connection yet on port " + this.tcpport + ", will retry...");
    skt = null;
    return; // Allow protocol handler loop to continue
}
```

**Why:** Timeouts are expected and normal behavior. Logging them as errors creates noise and hides real problems.

### Client Socket Read Timeout

**REQUIRED**: After accepting a connection, the client socket should use blocking reads (`setSoTimeout(0)`).

**Implementation:**
```java
skt.setTcpNoDelay(true);
skt.setSoTimeout(0); // No read timeout - block until data arrives
```

**Why:** Once connected, we want blocking reads to wait for data from the client. Timeouts on reads would cause premature disconnections.

### Connection State Management

**REQUIRED**: Always check socket state before reading/writing.

**Implementation:**
```java
if (skt != null)
{
    if (skt.isClosed() || !skt.isConnected())
    {
        logger.warn("Socket is closed or not connected, closing client");
        closeClient();
        // Try to get a new connection
        if (skt == null)
        {
            getClientConnection();
        }
    }
}
```

**Why:** Prevents attempting operations on invalid sockets, which causes exceptions and connection failures.

## Anti-Patterns (DO NOT DO)

### ❌ Blocking accept() without timeout
```java
// WRONG - blocks protocol handler loop indefinitely
skt = srvr.accept();
```

### ❌ Logging timeout exceptions as errors
```java
// WRONG - creates log noise
catch (SocketTimeoutException e) {
    logger.error("Timeout waiting for connection"); // NO!
}
```

### ❌ Setting read timeout on client socket
```java
// WRONG - causes premature disconnections
skt.setSoTimeout(10000); // NO! Use 0 for blocking reads
```

### ❌ Not resetting ServerSocket timeout after accept
```java
// WRONG - timeout persists for future accepts
srvr.setSoTimeout(5000);
skt = srvr.accept();
// Missing: srvr.setSoTimeout(0);
```

## Testing Requirements

When modifying `DWTCPDevice`:

1. **Test direct connection**: Client should connect immediately without workarounds
2. **Test connection retry**: If client connects during timeout, it should be accepted on next retry
3. **Test protocol handler responsiveness**: Handler should continue processing other operations while waiting for connection
4. **Test data flow**: Once connected, data should flow normally with blocking reads

## Historical Context

**The Workaround Problem:**
- Original issue: Blocking `accept()` prevented protocol handler from initializing
- Workaround required: Switch VCC to port 6800, then 6551, then restart CoCo
- Root cause: Protocol handler wasn't ready when client tried to connect
- Solution: 5-second timeout on `accept()` allows handler to initialize and retry

**Never revert to blocking accept()** - it breaks the connection flow and requires workarounds.

