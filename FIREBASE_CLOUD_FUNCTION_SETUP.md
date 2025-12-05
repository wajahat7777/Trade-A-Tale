# Firebase Cloud Function Setup for Push Notifications

## Step 1: Install Firebase CLI

1. Install Node.js (if not already installed): https://nodejs.org/
2. Install Firebase CLI globally:
   ```bash
   npm install -g firebase-tools
   ```
3. Login to Firebase:
   ```bash
   firebase login
   ```

## Step 2: Initialize Firebase Functions

1. In your project root directory, run:
   ```bash
   firebase init functions
   ```
2. Select:
   - Use an existing project: `tradeatale-8ddcb`
   - Language: JavaScript (or TypeScript if you prefer)
   - Install dependencies: Yes

## Step 3: Install Required Dependencies

Navigate to the `functions` folder and install dependencies:
```bash
cd functions
npm install firebase-admin firebase-functions
```

## Step 4: Create the Cloud Function

Replace the contents of `functions/index.js` with the code provided in `functions/index.js` file.

## Step 5: Deploy the Function

1. Make sure you're in the project root directory
2. Deploy the function:
   ```bash
   firebase deploy --only functions
   ```

## Step 6: Verify in Firebase Console

1. Go to Firebase Console → Functions
2. You should see `sendMessageNotification` function deployed
3. Check the logs to ensure it's working

## Alternative: Using Firebase Console (No Code Required)

If you don't want to set up Cloud Functions, you can use Firebase Console to send test notifications:

1. Go to Firebase Console → Cloud Messaging
2. Click "Send test message"
3. Enter FCM token (from your app's logs or database)
4. Send notification

## Testing

1. Send a message from one user to another
2. The receiver should receive a push notification
3. Check Firebase Console → Functions → Logs for any errors

