import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";

admin.initializeApp();

/**
 * Scheduled function that runs every 1 minute to check for pending notifications
 * in the 'scheduled_fcm' collection that are due to be sent.
 */
export const checkScheduledNotifications = functions.scheduler.onSchedule("every 1 minutes", async (event) => {
    const db = getFirestore("tongue-coins");
    const now = Date.now();

    console.log(`Checking for scheduled notifications due before: ${now}`);

    // Query pending notifications where scheduledTime <= now
    const snapshot = await db.collection("scheduled_fcm")
        .where("status", "==", "pending")
        .where("scheduledTime", "<=", now)
        .limit(100)
        .get();

    if (snapshot.empty) {
        console.log("No pending notifications due.");
        return;
    }

    console.log(`Found ${snapshot.size} notifications to send.`);

    const promises = snapshot.docs.map(async (doc) => {
        const docRef = doc.ref;

        try {
            // Use a transaction to ensure we only process 'pending' notifications
            // and immediately mark them as 'processing' to avoid duplicate sends.
            await db.runTransaction(async (transaction) => {
                const freshDoc = await transaction.get(docRef);
                const data = freshDoc.data();

                if (!data || data.status !== "pending") {
                    console.log(`Notification ${docRef.id} is no longer pending. Skipping.`);
                    return;
                }

                // Immediately mark as processing
                transaction.update(docRef, { status: "processing", processingAt: admin.firestore.FieldValue.serverTimestamp() });

                // Construct and send the message outside the transaction-specific logic (but within the promise)
                // Actually, for maximum safety, we do the update then the send.
            });

            // Re-fetch to confirm we are the ones who set it to processing (or just continue if transaction succeeded)
            const freshDoc = await docRef.get();
            const data = freshDoc.data();
            if (!data || data.status !== "processing") return;

            const baseMessage = {
                notification: {
                    title: data.title,
                    body: data.body,
                    imageUrl: data.imageUrl || undefined,
                },
                data: data.data || {},
                android: {
                    priority: (data.priority === "HIGH" ? "high" : "normal") as ("high" | "normal"),
                    notification: {
                        channelId: data.androidChannelId || "general",
                        sound: data.soundEnabled ? "default" : undefined,
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: data.soundEnabled ? "default" : undefined,
                            badge: data.badgeCount || undefined,
                        }
                    }
                }
            };

            let message: admin.messaging.Message;
            if (data.targetType === "topic") {
                message = { ...baseMessage, topic: data.targetValue };
            } else if (data.targetType === "token") {
                message = { ...baseMessage, token: data.targetValue };
            } else if (data.targetType === "condition") {
                message = { ...baseMessage, condition: data.targetValue };
            } else {
                throw new Error(`Unknown targetType: ${data.targetType}`);
            }

            const response = await admin.messaging().send(message);
            console.log(`Successfully sent message ${docRef.id}: ${response}`);

            await docRef.update({
                status: "sent",
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                fcmMessageId: response
            });

        } catch (error) {
            console.error(`Error processing message ${docRef.id}:`, error);
            await docRef.update({
                status: "failed",
                error: (error as Error).message,
                lastAttemptAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }
    });

    await Promise.all(promises);
});
