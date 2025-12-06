const axios = require('axios');
require('dotenv').config();

// Firebase Configuration
const FIREBASE_DB_URL = 'https://tradeatale-8ddcb-default-rtdb.firebaseio.com';
const FIREBASE_SERVER_KEY = process.env.FIREBASE_SERVER_KEY || '1ICNt9DQdsEZ6ezlxhuH3OzAFHiNREjlfPzlTt00';

// Helper function to make Firebase REST API calls
async function firebaseGet(path) {
  try {
    const url = `${FIREBASE_DB_URL}/${path}.json`;
    const response = await axios.get(url);
    return response.data;
  } catch (error) {
    console.error(`Error reading from Firebase ${path}:`, error.message);
    return null;
  }
}

async function firebaseSet(path, value) {
  try {
    const url = `${FIREBASE_DB_URL}/${path}.json`;
    await axios.put(url, value);
    return true;
  } catch (error) {
    console.error(`Error writing to Firebase ${path}:`, error.message);
    return false;
  }
}

async function firebaseDelete(path) {
  try {
    const url = `${FIREBASE_DB_URL}/${path}.json`;
    await axios.delete(url);
    return true;
  } catch (error) {
    console.error(`Error deleting from Firebase ${path}:`, error.message);
    return false;
  }
}

// Polling interval for checking new messages (in milliseconds)
const POLL_INTERVAL = 2000; // Check every 2 seconds
let lastMessageTimestamps = new Map(); // Track last seen message timestamps per conversation
let processedBarterRequests = new Set(); // Track processed barter request IDs (ownerId_requestId)
let lastLoginTimestamps = new Map(); // Track last seen login timestamps

// OneSignal Configuration
const ONESIGNAL_APP_ID = 'c9f696d4-3e2f-4de4-9308-26c282b08ed3';
const ONESIGNAL_REST_API_KEY = process.env.ONESIGNAL_REST_API_KEY; // Set this in .env file

// Function to send OneSignal notification
async function sendOneSignalNotification(playerId, title, message, additionalData = {}) {
  if (!ONESIGNAL_REST_API_KEY) {
    console.error('OneSignal REST API Key not set! Please set ONESIGNAL_REST_API_KEY in .env file');
    return;
  }

  const notification = {
    app_id: ONESIGNAL_APP_ID,
    include_player_ids: [playerId],
    headings: { en: title },
    contents: { en: message },
    data: additionalData
  };

  try {
    const response = await axios.post('https://onesignal.com/api/v1/notifications', notification, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${ONESIGNAL_REST_API_KEY.trim()}`
      }
    });
    console.log('Notification sent successfully to player:', playerId);
  } catch (error) {
    if (error.response?.status === 401) {
      console.error('OneSignal Authentication Error: Invalid REST API Key. Please check your .env file.');
    } else {
      console.error('Error sending notification:', error.response?.data || error.message);
    }
  }
}

// Function to get user's OneSignal Player ID from Firebase
async function getPlayerId(userId) {
  try {
    const data = await firebaseGet(`oneSignalPlayerIds/${userId}`);
    return data;
  } catch (error) {
    console.error(`Error getting Player ID for user ${userId}:`, error);
    return null;
  }
}

// Function to get user's name from Firebase
async function getUserName(userId) {
  try {
    const data = await firebaseGet(`users/${userId}/name`);
    return data || 'Someone';
  } catch (error) {
    console.error(`Error getting user name for ${userId}:`, error);
    return 'Someone';
  }
}

// Function to check for new messages
async function checkForNewMessages() {
  try {
    const chats = await firebaseGet('Chats');
    if (!chats) return;
    
    for (const [conversationId, conversationData] of Object.entries(chats)) {
      if (!conversationData || !conversationData.messages) continue;
      
      const messages = conversationData.messages;
      const lastTimestamp = lastMessageTimestamps.get(conversationId) || 0;
      
      let maxTimestamp = lastTimestamp;
      
      for (const [messageId, message] of Object.entries(messages)) {
        if (!message || !message.timestamp) continue;
        
        const timestamp = typeof message.timestamp === 'object' 
          ? (message.timestamp.val || Date.now())
          : (typeof message.timestamp === 'number' ? message.timestamp : Date.now());
        
        // Only process messages newer than what we've seen
        if (timestamp > lastTimestamp) {
          const senderId = message.senderId;
          const receiverId = message.receiverId;
          const text = message.text;
          
          if (senderId && receiverId && text) {
            // Get receiver's Player ID
            const playerId = await getPlayerId(receiverId);
            if (playerId) {
              // Get sender's name
              const senderName = await getUserName(senderId);
              
              // Send notification
              await sendOneSignalNotification(
                playerId,
                `New message from ${senderName}`,
                text.length > 50 ? text.substring(0, 50) + '...' : text,
                {
                  type: 'message',
                  senderId: senderId,
                  conversationId: conversationId
                }
              );
            }
          }
          
          // Track the maximum timestamp we've seen
          if (timestamp > maxTimestamp) {
            maxTimestamp = timestamp;
          }
        }
      }
      
      // Update last timestamp for this conversation
      if (maxTimestamp > lastTimestamp) {
        lastMessageTimestamps.set(conversationId, maxTimestamp);
      }
    }
  } catch (error) {
    console.error('Error checking for new messages:', error);
  }
}

// Function to check for new barter requests
async function checkForNewBarterRequests() {
  try {
    const barterRequests = await firebaseGet('BarterRequest');
    if (!barterRequests) return;
    
    for (const [ownerId, requests] of Object.entries(barterRequests)) {
      if (!requests) continue;
      
      for (const [requestId, request] of Object.entries(requests)) {
        if (!request || !request.yourID) continue;
        
        // Create unique identifier for this request
        const requestKey = `${ownerId}_${requestId}`;
        
        // Check if we've already processed this request
        if (!processedBarterRequests.has(requestKey)) {
          const requesterId = request.yourID;
          
          // Get owner's Player ID
          const playerId = await getPlayerId(ownerId);
          if (playerId) {
            // Get requester's name
            const requesterName = await getUserName(requesterId);
            
            // Send notification
            await sendOneSignalNotification(
              playerId,
              'New Barter Request',
              `${requesterName} wants to barter with you!`,
              {
                type: 'barter_request',
                requesterId: requesterId
              }
            );
          }
          
          // Mark this request as processed
          processedBarterRequests.add(requestKey);
        }
      }
    }
  } catch (error) {
    console.error('Error checking for new barter requests:', error);
  }
}

// Function to check for new login events
async function checkForNewLogins() {
  try {
    const logins = await firebaseGet('userLogins');
    if (!logins) return;
    
    for (const [userId, loginData] of Object.entries(logins)) {
      if (!loginData) continue;
      
      const lastTimestamp = lastLoginTimestamps.get(userId);
      const loginTimestamp = loginData.timestamp || Date.now();
      
      // If this is a new login we haven't processed
      if (!lastTimestamp || loginTimestamp > lastTimestamp) {
        // Get user's Player ID
        const playerId = await getPlayerId(userId);
        if (playerId) {
          // Send welcome notification
          await sendOneSignalNotification(
            playerId,
            'Welcome to Trade A Tale!',
            'You have successfully logged in. Start trading books now!',
            {
              type: 'login'
            }
          );
          
          // Clean up the login event after sending notification
          await firebaseDelete(`userLogins/${userId}`);
        }
        
        lastLoginTimestamps.set(userId, loginTimestamp);
      }
    }
  } catch (error) {
    console.error('Error checking for new logins:', error);
  }
}

// Main polling loop
async function startPolling() {
  console.log('Notification server started! Polling for events...');
  
  // Initial check
  await checkForNewMessages();
  await checkForNewBarterRequests();
  await checkForNewLogins();
  
  // Set up interval to poll for changes
  setInterval(async () => {
    await checkForNewMessages();
    await checkForNewBarterRequests();
    await checkForNewLogins();
  }, POLL_INTERVAL);
}

// Start the server
startPolling();

