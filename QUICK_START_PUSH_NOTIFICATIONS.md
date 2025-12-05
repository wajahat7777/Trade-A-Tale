# Quick Start: Push Notifications Setup

## What You Need to Do

Your app is already set up to **receive** push notifications. Now you need to set up the **backend** to **send** them.

## Option 1: Firebase Cloud Functions (Recommended - Free Tier Available)

### Step 1: Install Prerequisites
```bash
# Install Node.js from https://nodejs.org/
# Then install Firebase CLI
npm install -g firebase-tools
```

### Step 2: Login to Firebase
```bash
firebase login
```

### Step 3: Initialize Functions
```bash
# In your project root directory
firebase init functions
```
- Select: **Use an existing project**
- Choose: **tradeatale-8ddcb**
- Language: **JavaScript**
- Install dependencies: **Yes**

### Step 4: Copy the Function Code
The `functions/index.js` file has been created for you. It contains:
- `sendMessageNotification` - Sends notification when a message is sent
- `sendBarterRequestNotification` - Sends notification when a barter request is created

### Step 5: Install Dependencies
```bash
cd functions
npm install
```

### Step 6: Deploy
```bash
# Go back to project root
cd ..
firebase deploy --only functions
```

### Step 7: Test
1. Send a message from one user to another
2. The receiver should get a push notification
3. Check Firebase Console → Functions → Logs for any errors

## Option 2: Manual Testing (No Code Required)

You can test notifications manually:

1. **Get FCM Token:**
   - Run your app
   - Check Firebase Console → Realtime Database → `userTokens`
   - Copy a user's FCM token

2. **Send Test Notification:**
   - Go to Firebase Console → Cloud Messaging
   - Click "Send test message"
   - Paste the FCM token
   - Enter title and message
   - Click "Test"

## Option 3: Use Firebase Console Cloud Messaging

1. Go to Firebase Console → Cloud Messaging
2. Click "Send your first message"
3. Create notification campaigns
4. Target specific users or all users

## Troubleshooting

### Notifications Not Appearing?
1. Check if FCM token is saved in database: `userTokens/{userId}`
2. Check notification permissions in Android settings
3. Check Firebase Console → Functions → Logs for errors
4. Verify `google-services.json` is in `app/` folder

### Function Deployment Failed?
1. Make sure you're logged in: `firebase login`
2. Check billing is enabled (Blaze plan required for Cloud Functions)
3. Verify Node.js version: `node --version` (should be 18+)

### Need Help?
- Check `FIREBASE_CLOUD_FUNCTION_SETUP.md` for detailed instructions
- Firebase Documentation: https://firebase.google.com/docs/cloud-messaging

## Current Status

✅ **App Side (Done):**
- FCM service registered
- Token saving to database
- Notification display handler
- Click to open chat

⏳ **Backend Side (You Need to Do):**
- Deploy Cloud Functions (Option 1)
- Or use manual testing (Option 2)

