import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";

admin.initializeApp();

/**
 * Scheduled function that runs every 1 minute to process pending FCM notifications
 * from the 'scheduled_fcm' Firestore collection.
 *
 * Flow per document:
 *   pending → processing (transaction) → sent | failed
 *
 * The transaction guards against double-send when multiple function instances run
 * simultaneously (Cloud Functions may have concurrent executions).
 */
export const checkScheduledNotifications = functions.scheduler.onSchedule(
    "every 1 minutes",
    async () => {
        const db = getFirestore("tongue-coins");
        const now = Date.now();

        const snapshot = await db
            .collection("scheduled_fcm")
            .where("status", "==", "pending")
            .where("scheduledTime", "<=", now)
            .limit(100)
            .get();

        if (snapshot.empty) return;

        console.log(`Processing ${snapshot.size} scheduled notification(s).`);

        await Promise.all(snapshot.docs.map((doc) => processDoc(doc.ref, db)));
    }
);

async function processDoc(
    docRef: FirebaseFirestore.DocumentReference,
    db: FirebaseFirestore.Firestore
): Promise<void> {
    try {
        // Atomically claim the document to prevent duplicate sends across concurrent executions.
        await db.runTransaction(async (tx) => {
            const fresh = await tx.get(docRef);
            const data = fresh.data();
            if (!data || data.status !== "pending") return;
            tx.update(docRef, {
                status: "processing",
                processingAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        });

        const snap = await docRef.get();
        const data = snap.data();
        if (!data || data.status !== "processing") return;

        const message = buildMessage(data);
        const fcmMessageId = await admin.messaging().send(message);

        console.log(`Sent ${docRef.id}: ${fcmMessageId}`);
        await docRef.update({
            status: "sent",
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            fcmMessageId,
        });
    } catch (error) {
        console.error(`Failed ${docRef.id}:`, error);
        await docRef.update({
            status: "failed",
            error: (error as Error).message,
            lastAttemptAt: admin.firestore.FieldValue.serverTimestamp(),
        });
    }
}

function buildMessage(data: FirebaseFirestore.DocumentData): admin.messaging.Message {
    const isHighPriority = data.priority === "HIGH";
    const channelId: string = data.androidChannelId || "fcm_default_channel";

    const baseMessage: Omit<admin.messaging.Message, "token" | "topic" | "condition"> = {
        notification: {
            title: data.title,
            body: data.body,
            ...(data.imageUrl ? { imageUrl: data.imageUrl } : {}),
        },
        ...(data.data && Object.keys(data.data).length > 0 ? { data: data.data } : {}),
        android: {
            priority: isHighPriority ? "high" : "normal",
            ttl: 604800000, // 7 days in ms
            notification: {
                channelId,
                sound: data.soundEnabled ? "default" : undefined,
                ...(data.imageUrl ? { imageUrl: data.imageUrl } : {}),
            },
        },
        apns: {
            headers: {
                "apns-priority": isHighPriority ? "10" : "5",
            },
            payload: {
                aps: {
                    sound: data.soundEnabled ? "default" : undefined,
                    badge: data.badgeCount || undefined,
                    // mutableContent enables NSE for rich notifications (images) on iOS
                    ...(data.imageUrl ? { mutableContent: true } : {}),
                },
            },
        },
        fcmOptions: {
            analyticsLabel: data.analyticsLabel || "scheduled",
        },
    };

    if (data.targetType === "topic") {
        return { ...baseMessage, topic: data.targetValue };
    } else if (data.targetType === "token") {
        return { ...baseMessage, token: data.targetValue };
    } else if (data.targetType === "condition") {
        return { ...baseMessage, condition: data.targetValue };
    }
    throw new Error(`Unknown targetType: ${data.targetType}`);
}
