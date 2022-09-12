#!/bin/sh
#0 4 * * * /biscon/sysgateii/autosvr/bin/clearfile.sh
find /log/bis/autosvr/archive/ -mtime +15 -name '*' -exec rm -rf {} \;
