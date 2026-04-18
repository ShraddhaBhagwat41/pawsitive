# Real-Time NGO Notification Functions

This folder now includes a Firestore-triggered FCM function in `firebase_functions_example.js` (re-exported by `index.js`).

## Trigger

- Firestore document create: `reports/{reportId}`
- Function: `notifyNearbyNgosOnReportCreate`

## What It Does

1. Reads the new report location.
2. Loads NGO tokens from both `ngos` and `ngo_profiles`.
3. Filters verified NGOs within 10 km.
4. Sends high-priority FCM notification with default sound.
5. Writes delivery metadata back to the report:
   - `notifiedNGOs`
   - `notificationMeta.sentCount/successCount/failureCount`

## Payload Sent To NGO App

- Notification:
  - Title: `🚨 Animal in Need Nearby`
  - Body: `A new incident has been reported near your location`
  - Sound: `default`
- Data:
  - `reportId`
  - `animalType`
  - `condition`
  - `lat`
  - `lng`
  - `location` (JSON string)

## Deploy Notes

If using Firebase Functions deploy flow, ensure your functions source points to this folder and run:

```bash
npm install
firebase deploy --only functions
```

If your Firebase config uses another source directory, copy `index.js` and `firebase_functions_example.js` there.
