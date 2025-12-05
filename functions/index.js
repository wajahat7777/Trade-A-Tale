const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Cloud Function to send push notification when a new message is created
exports.sendMessageNotification = functions.database
  .ref('/Chats/{conversationId}/messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    const messageData = snapshot.val();
    const conversationId = context.params.conversationId;
    const messageId = context.params.messageId;

    // Get sender and receiver IDs
    const senderId = messageData.senderId;
    const receiverId = messageData.receiverId;
    const messageText = messageData.text || 'New message';

    if (!receiverId || !senderId) {
      console.log('Missing sender or receiver ID');
      return null;
    }

    // Get receiver's FCM token
    const receiverTokenSnapshot = await admin
      .database()
      .ref(`/userTokens/${receiverId}`)
      .once('value');

    const receiverToken = receiverTokenSnapshot.val();

    if (!receiverToken) {
      console.log('No FCM token found for receiver:', receiverId);
      return null;
    }

    // Get sender's name
    const senderSnapshot = await admin
      .database()
      .ref(`/users/${senderId}/name`)
      .once('value');

    const senderName = senderSnapshot.val() || 'Someone';

    // Prepare notification payload
    const payload = {
      notification: {
        title: senderName,
        body: messageText.length > 50 ? messageText.substring(0, 50) + '...' : messageText,
        sound: 'default',
      },
      data: {
        type: 'message',
        conversationId: conversationId,
        senderId: senderId,
        messageId: messageId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK', // For Android
      },
      token: receiverToken,
    };

    // Send notification
    try {
      const response = await admin.messaging().send(payload);
      console.log('Successfully sent message notification:', response);
      return response;
    } catch (error) {
      console.error('Error sending message notification:', error);
      return null;
    }
  });

// Optional: Function to send notification when a barter request is created
exports.sendBarterRequestNotification = functions.database
  .ref('/BarterRequest/{userId}/{requestId}')
  .onCreate(async (snapshot, context) => {
    const requestData = snapshot.val();
    const userId = context.params.userId;
    const requestId = context.params.requestId;

    // Get requester's name
    const requesterId = requestData.yourID;
    if (!requesterId) {
      return null;
    }

    const requesterSnapshot = await admin
      .database()
      .ref(`/users/${requesterId}/name`)
      .once('value');

    const requesterName = requesterSnapshot.val() || 'Someone';

    // Get receiver's FCM token
    const receiverTokenSnapshot = await admin
      .database()
      .ref(`/userTokens/${userId}`)
      .once('value');

    const receiverToken = receiverTokenSnapshot.val();

    if (!receiverToken) {
      console.log('No FCM token found for user:', userId);
      return null;
    }

    // Prepare notification payload
    const payload = {
      notification: {
        title: 'New Barter Request',
        body: `${requesterName} wants to trade with you`,
        sound: 'default',
      },
      data: {
        type: 'barter_request',
        requestId: requestId,
        requesterId: requesterId,
      },
      token: receiverToken,
    };

    // Send notification
    try {
      const response = await admin.messaging().send(payload);
      console.log('Successfully sent barter request notification:', response);
      return response;
    } catch (error) {
      console.error('Error sending barter request notification:', error);
      return null;
    }
  });

