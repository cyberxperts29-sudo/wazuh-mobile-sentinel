# Wazuh Mobile Sentinel - Backend Receiver Status

## Goal
Small Flask app that receives JSON payloads from the Android collector app 
via POST /ingest, validates them minimally, and appends each payload as a 
single line to a log file. A Wazuh agent (running on the same machine later, 
e.g. EC2) will monitor that log file and forward entries to the Wazuh manager.

## Current phase
Phase 5: Basic receiver - DONE

## Next steps
Test end-to-end: run Flask locally, send test payload from Android emulator, 
verify log file gets a valid JSON line appended. After that: prepare for EC2 
deployment.
