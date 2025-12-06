# Trade A Tale Notification Server

This Node.js server listens to Firebase Realtime Database changes and automatically sends push notifications via OneSignal.

## Setup Instructions

### 1. Install Dependencies
```bash
npm install
```

### 2. Firebase Server Key (Already Configured)
The Firebase Server Key is already set in the code: `1ICNt9DQdsEZ6ezlxhuH3OzAFHiNREjlfPzlTt00`

If you need to change it, you can set it in the `.env` file:
```
FIREBASE_SERVER_KEY=your_firebase_server_key_here
```

**Note**: Make sure your Firebase Realtime Database rules allow reading. The server uses REST API to read from the database.

### 3. Get OneSignal REST API Key
1. Go to [OneSignal Dashboard](https://onesignal.com/)
2. Select your app: **Trade A Tale**
3. Go to **Settings** → **Keys & IDs**
4. Copy the **REST API Key**
5. Create a `.env` file in this directory:
```bash
ONESIGNAL_REST_API_KEY=your_rest_api_key_here
```

### 4. Run the Server
```bash
npm start
```

The server will now:
- ✅ Send notifications when users receive messages
- ✅ Send notifications when users receive barter requests
- ✅ Send welcome notifications when users log in

## Keep Server Running

For production, you can:
- Use **PM2**: `npm install -g pm2 && pm2 start server.js`
- Use **Heroku**, **Railway**, or **Render** to host it
- Use a **VPS** (DigitalOcean, AWS, etc.)

## Testing

1. Make sure the server is running
2. Log in to the app - you should receive a welcome notification
3. Send a message to another user - they should receive a notification
4. Send a barter request - the owner should receive a notification

## How It Works

The server uses **polling** (checks every 2 seconds) instead of real-time listeners because we're using Firebase REST API instead of Admin SDK. This is less efficient but works without requiring a service account JSON file.

## Firebase Database Rules

Make sure your Firebase Realtime Database rules allow reading. In Firebase Console → Realtime Database → Rules, you should have:

```json
{
  "rules": {
    ".read": true,
    ".write": "auth != null"
  }
}
```

Or more secure:
```json
{
  "rules": {
    "Chats": {
      ".read": true
    },
    "BarterRequest": {
      ".read": true
    },
    "userLogins": {
      ".read": true,
      ".write": "auth != null"
    },
    "oneSignalPlayerIds": {
      ".read": true
    },
    "users": {
      ".read": true
    }
  }
}
```

