#!/bin/sh
# Entrypoint script for configurable JVM settings
#
# Optimized defaults for 2GB App Runner instances processing notebooks up to 10MB
# Heap: 1280MB max (leaves 768MB for off-heap: metaspace, direct buffers, thread stacks)
#
# For 4GB instances, set environment variables:
#   JVM_MAX_HEAP=2560m
#   JVM_MIN_HEAP=768m
#   JVM_MAX_METASPACE=384m

# JVM Heap Settings
JVM_MAX_HEAP=${JVM_MAX_HEAP:-1280m}
JVM_MIN_HEAP=${JVM_MIN_HEAP:-512m}
JVM_MAX_METASPACE=${JVM_MAX_METASPACE:-256m}

# G1GC Tuning
JVM_MAX_GC_PAUSE_MILLIS=${JVM_MAX_GC_PAUSE_MILLIS:-200}
JVM_G1_HEAP_REGION_SIZE=${JVM_G1_HEAP_REGION_SIZE:-4m}

# Execute the application
exec java \
    -Xmx"${JVM_MAX_HEAP}" \
    -Xms"${JVM_MIN_HEAP}" \
    -XX:MaxMetaspaceSize="${JVM_MAX_METASPACE}" \
    -XX:NativeMemoryTracking=summary \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis="${JVM_MAX_GC_PAUSE_MILLIS}" \
    -XX:G1HeapRegionSize="${JVM_G1_HEAP_REGION_SIZE}" \
    -XX:+UseStringDeduplication \
    -jar /app/sharing-service.jar
