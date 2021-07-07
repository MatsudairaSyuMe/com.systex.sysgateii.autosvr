Auto printer service program

 based on High-performance non-blocking, multi-endpoint connection pool

  2020/1/28

  Matsudaira SyuMe

# Introduction

Currently Netty doesn't have a connection pool implementation which
would allow to get a connection to any endpoint from the specified set.
This connection pool implementation solves the problem using
round-robin endpoint selection for each connection leased from the pool.

Also, this connection pool implementation supports the batch mode
operation (leasing/releasing many connections at once).

# Usage

## Code Example

```
  java

final Bootstrap bootstrap = new Bootstrap();
// configure the Netty's bootstrap instance here

// your custom channel pool handler
final ChannelPoolHandler cph = ...

final NonBlockingConnPool connPool = new BasicMultiNodeConnPool(
    storageNodeAddrs, bootstrap, cph, storageNodePort, connAttemptsLimit
);

// optional
connPool.preCreateConnections(concurrencyLevel);

// use the pool
final Channel conn = connPool.lease();
...
connPool.release(conn);

// don't forget to clean up
connPool.close();
```

## Batch Mode

```
  java
...

// try to get up to 4096 connections per time
final int maxConnCount = 0x1000;
final List<Channel> conns = new ArrayList<>(maxConnCount);
final int availConnCount = connPool.lease(conns, maxConnCount);

// use the obtained connections
Channel conn;
for(int i = 0; i < availConnCount; i ++) {
    conn = conns.get(i);
    // use the connection
}

// release all connections back into the pool
connPool.lease(conns);
```


