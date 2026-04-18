const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const SEARCH_RADIUS_METERS = 10000;

function toNumber(value) {
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }
  return null;
}

function haversineMeters(lat1, lng1, lat2, lng2) {
  const R = 6371e3;
  const p1 = (lat1 * Math.PI) / 180;
  const p2 = (lat2 * Math.PI) / 180;
  const dp = ((lat2 - lat1) * Math.PI) / 180;
  const dl = ((lng2 - lng1) * Math.PI) / 180;

  const a =
    Math.sin(dp / 2) * Math.sin(dp / 2) +
    Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

async function loadNgosWithTokens() {
  const [ngosSnap, ngoProfilesSnap] = await Promise.all([
    db.collection("ngos").where("isVerified", "==", true).get().catch(() => null),
    db.collection("ngo_profiles").get(),
  ]);

  const ngoMap = new Map();

  if (ngosSnap && !ngosSnap.empty) {
    ngosSnap.forEach((doc) => {
      const data = doc.data() || {};
      const location = data.location || {};
      ngoMap.set(doc.id, {
        id: doc.id,
        fcmToken: data.fcmToken || null,
        lat: toNumber(location.lat),
        lng: toNumber(location.lng),
        isVerified: data.isVerified === true,
      });
    });
  }

  if (ngoProfilesSnap && !ngoProfilesSnap.empty) {
    ngoProfilesSnap.forEach((doc) => {
      const data = doc.data() || {};
      const existing = ngoMap.get(doc.id) || { id: doc.id };
      const lat = toNumber(data.latitude);
      const lng = toNumber(data.longitude);

      ngoMap.set(doc.id, {
        id: doc.id,
        fcmToken: existing.fcmToken || data.fcmToken || null,
        lat: existing.lat != null ? existing.lat : lat,
        lng: existing.lng != null ? existing.lng : lng,
        isVerified: existing.isVerified === true || data.verification_status === "VERIFIED",
      });
    });
  }

  return Array.from(ngoMap.values()).filter((ngo) => ngo.fcmToken && ngo.isVerified);
}

exports.notifyNearbyNgosOnReportCreate = functions.firestore
  .document("reports/{reportId}")
  .onCreate(async (snap, context) => {
    const report = snap.data() || {};
    const reportId = context.params.reportId;

    const location = report.location || {};
    const incidentLat = toNumber(location.lat);
    const incidentLng = toNumber(location.lng);

    if (incidentLat == null || incidentLng == null) {
      console.log("Skipping notification: report has no valid lat/lng", reportId);
      return null;
    }

    const animalType = report.animalType || "Animal";
    const condition = report.condition || "Unknown";

    const ngos = await loadNgosWithTokens();

    const targetNgos = ngos.filter((ngo) => {
      if (ngo.lat == null || ngo.lng == null) return false;
      const distance = haversineMeters(incidentLat, incidentLng, ngo.lat, ngo.lng);
      return distance <= SEARCH_RADIUS_METERS;
    });

    if (targetNgos.length === 0) {
      await snap.ref.set(
        {
          notifiedNGOs: [],
          notificationMeta: {
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            sentCount: 0,
            successCount: 0,
            failureCount: 0,
            message: "No nearby verified NGOs",
          },
        },
        { merge: true }
      );
      return null;
    }

    const tokens = targetNgos.map((ngo) => ngo.fcmToken);
    const ngoIds = targetNgos.map((ngo) => ngo.id);

    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "🚨 Animal in Need Nearby",
        body: "A new incident has been reported near your location",
      },
      data: {
        reportId,
        animalType: String(animalType),
        condition: String(condition),
        location: JSON.stringify(location),
        lat: String(incidentLat),
        lng: String(incidentLng),
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
          channelId: "fcm_default_channel",
        },
      },
    });

    const cleanupPromises = [];
    response.responses.forEach((r, index) => {
      if (
        !r.success &&
        r.error &&
        (r.error.code === "messaging/registration-token-not-registered" ||
          r.error.code === "messaging/invalid-registration-token")
      ) {
        const ngoId = ngoIds[index];
        cleanupPromises.push(
          db.collection("ngos").doc(ngoId).set({ fcmToken: admin.firestore.FieldValue.delete() }, { merge: true })
        );
        cleanupPromises.push(
          db.collection("ngo_profiles").doc(ngoId).set({ fcmToken: admin.firestore.FieldValue.delete() }, { merge: true })
        );
      }
    });
    await Promise.all(cleanupPromises);

    await snap.ref.set(
      {
        notifiedNGOs: ngoIds,
        notificationMeta: {
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          sentCount: tokens.length,
          successCount: response.successCount,
          failureCount: response.failureCount,
        },
      },
      { merge: true }
    );

    return null;
  });

exports.notifyStaffOnAssignment = functions.firestore
  .document("reports/{reportId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data() || {};
    const after = change.after.data() || {};

    const beforeAssigned = before.assignedTo && before.assignedTo.staffId;
    const afterAssigned = after.assignedTo && after.assignedTo.staffId;
    const status = (after.status || "").toString().toLowerCase();

    if (!afterAssigned || afterAssigned === beforeAssigned) {
      return null;
    }

    if (status !== "assigned") {
      return null;
    }

    const reportId = context.params.reportId;

    const staffDocCandidates = await Promise.all([
      db.collection("staff").doc(afterAssigned).get().catch(() => null),
      db.collection("ngo_staff").doc(afterAssigned).get().catch(() => null),
      db.collection("users").doc(afterAssigned).get().catch(() => null),
    ]);

    let token = null;
    for (const doc of staffDocCandidates) {
      if (doc && doc.exists && doc.data() && doc.data().fcmToken) {
        token = doc.data().fcmToken;
        break;
      }
    }

    if (!token) {
      console.log("No FCM token found for assigned staff", afterAssigned);
      return null;
    }

    const location = after.location || {};

    await admin.messaging().send({
      token,
      notification: {
        title: "🚨 Rescue Assigned",
        body: "You have been assigned a rescue case",
      },
      data: {
        reportId,
        type: "ASSIGNED",
        animalType: String(after.animalType || "Animal"),
        condition: String(after.condition || "Unknown"),
        location: JSON.stringify(location),
        lat: String(location.lat || ""),
        lng: String(location.lng || ""),
      },
      android: {
        priority: "high",
        notification: {
          sound: "emergency_siren",
          channelId: "fcm_default_channel",
        },
      },
    });

    return null;
  });

