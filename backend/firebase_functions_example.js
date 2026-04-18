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
    const p1 = lat1 * Math.PI / 180;
    const p2 = lat2 * Math.PI / 180;
    const dp = (lat2 - lat1) * Math.PI / 180;
    const dl = (lng2 - lng1) * Math.PI / 180;

    const a = Math.sin(dp / 2) * Math.sin(dp / 2)
        + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

async function loadNgosWithTokens() {
    const [ngosSnap, ngoProfilesSnap] = await Promise.all([
        db.collection("ngos").where("isVerified", "==", true).get().catch(() => null),
        db.collection("ngo_profiles").get()
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
                isVerified: data.isVerified === true
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
                isVerified: existing.isVerified === true || data.verification_status === "VERIFIED"
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
            await snap.ref.set({
                notifiedNGOs: [],
                notificationMeta: {
                    sentAt: admin.firestore.FieldValue.serverTimestamp(),
                    sentCount: 0,
                    successCount: 0,
                    failureCount: 0,
                    message: "No nearby verified NGOs"
                }
            }, { merge: true });
            return null;
        }

        const tokens = targetNgos.map((ngo) => ngo.fcmToken);
        const ngoIds = targetNgos.map((ngo) => ngo.id);

        const payload = {
            notification: {
                title: "🚨 Animal in Need Nearby",
                body: "A new incident has been reported near your location"
            },
            data: {
                reportId,
                animalType: String(animalType),
                condition: String(condition),
                location: JSON.stringify(location),
                lat: String(incidentLat),
                lng: String(incidentLng)
            },
            android: {
                priority: "high",
                notification: {
                    sound: "default",
                    channelId: "fcm_default_channel"
                }
            }
        };

        const response = await admin.messaging().sendEachForMulticast({
            tokens,
            ...payload
        });

        // Remove invalid tokens from both collections.
        const cleanupPromises = [];
        response.responses.forEach((r, index) => {
            if (!r.success && r.error && (
                r.error.code === "messaging/registration-token-not-registered" ||
                r.error.code === "messaging/invalid-registration-token"
            )) {
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

        await snap.ref.set({
            notifiedNGOs: ngoIds,
            notificationMeta: {
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                sentCount: tokens.length,
                successCount: response.successCount,
                failureCount: response.failureCount
            }
        }, { merge: true });

        return null;
    });
