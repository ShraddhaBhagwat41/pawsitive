const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyNGOsOnNewReport = functions.firestore
    .document("incidents/{incidentId}")
    .onCreate(async (snap, context) => {
        const newReport = snap.data();
        console.log("New report detected:", newReport);

        // 1. You could query nearby NGOs based on location logic
        // For simplicity, we fetch all NGOs with an fcmToken or apply basic bounds filtering
        const db = admin.firestore();
        const ngosSnapshot = await db.collection("ngo_profiles").get();

        const tokens = [];
        ngosSnapshot.forEach((doc) => {
            const ngoData = doc.data();
            if (ngoData.fcmToken) {
                // Here you can calculate distance between ngoData.latitude/longitude
                // and newReport.location.lat/lng and push to token array if within radius
                tokens.push(ngoData.fcmToken);
            }
        });

        if (tokens.length === 0) {
            console.log("No NGO tokens found.");
            return null;
        }

        // 2. Prepare Notification Payload
        const animalType = newReport.animalType || "An animal";
        const payload = {
            notification: {
                title: "🚨 Emergency: " + animalType + " in Need!",
                body: "A new incident has been reported near your location.",
            },
            data: {
                reportId: context.params.incidentId,
                type: "NEW_REPORT"
            }
        };

        // 3. Send to tokens
        try {
            const response = await admin.messaging().sendToDevice(tokens, payload);
            console.log("Successfully sent notifications:", response);
        } catch (error) {
            console.error("Error sending notifications:", error);
        }

        return null;
    });

exports.notifyUserOnAccept = functions.firestore
    .document("incidents/{incidentId}")
    .onUpdate(async (change, context) => {
        const newValue = change.after.data();
        const previousValue = change.before.data();

        // Check if status changed to ACCEPTED
        if (newValue.status === "ACCEPTED" && previousValue.status !== "ACCEPTED") {
            const userId = newValue.reportedBy; // Assuming the reporter's ID is saved
            if (!userId) return null;

            const userDoc = await admin.firestore().collection("users").doc(userId).get();
            if (!userDoc.exists) return null;

            const userData = userDoc.data();
            if (userData.fcmToken) {
                const payload = {
                    notification: {
                        title: "Help is on the way! 🚑",
                        body: "An NGO has accepted your rescue report.",
                    },
                    data: {
                        reportId: context.params.incidentId,
                        type: "NGO_ACCEPTED"
                    }
                };

                try {
                    await admin.messaging().sendToDevice(userData.fcmToken, payload);
                } catch (error) {
                    console.error("Error sending accept notification:", error);
                }
            }
        }
        return null;
    });
