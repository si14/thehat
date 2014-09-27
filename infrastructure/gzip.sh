#!/bin/bash

for i in /opt/thehat/resources/public/css/*; do [ "${i:(-3)}" == ".gz" ] || gzip -c -9 "$i" > "$i.gz"; done
for i in /opt/thehat/resources/public/js/*; do [ "${i:(-3)}" == ".gz" ] || gzip -c -9 "$i" > "$i.gz"; done
