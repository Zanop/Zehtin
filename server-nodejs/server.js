const express = require('express');
const { WebSocketServer } = require('ws');
const { v4: uuidv4 } = require('uuid');
const http = require('http');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');

// Initialize Firebase Admin
try {
    const serviceAccount = require("./serviceAccountKey.json");
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log(`[${new Date().toISOString()}] Firebase Admin initialized successfully`);
} catch (e) {
    console.warn(`[${new Date().toISOString()}] Firebase Admin warning: serviceAccountKey.json not found or invalid. Notifications will be disabled.`);
}

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// File upload setup
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, 'uploads/');
    },
    filename: (req, file, cb) => {
        const uniqueId = uuidv4();
        const ext = path.extname(file.originalname);
        cb(null, `${uniqueId}${ext}`);
    }
});

const upload = multer({
    storage,
    limits: { fileSize: 20 * 1024 * 1024 }, // 20MB max
    fileFilter: (req, file, cb) => {
        const allowed = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        ];
        if (allowed.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('File type not allowed'));
        }
    }
});

const PORT = 12345;
const INVITE_CODE = 'zehtin2024';
const OFFLINE_TIMEOUT = 60000; // 1 minute grace period before marking offline

// Store connected clients and messages
const clients = new Map(); // deviceId -> { ws, name, deviceId, socketId }
const MESSAGES_FILE = './messages.json';
const TOKENS_FILE = './tokens.json';
const MEMBERS_FILE = './members.json';
const MAX_MESSAGES = 1000;

let messages = [];
let fcmTokens = {}; // deviceId -> token
let knownMembers = {}; // deviceId -> { name, lastSeen, isOnline }

// Load data from disk
try {
    if (fs.existsSync(MESSAGES_FILE)) {
        messages = JSON.parse(fs.readFileSync(MESSAGES_FILE, 'utf8'));
        console.log(`[${new Date().toISOString()}] Loaded ${messages.length} messages from disk`);
    }
    if (fs.existsSync(TOKENS_FILE)) {
        fcmTokens = JSON.parse(fs.readFileSync(TOKENS_FILE, 'utf8'));
        console.log(`[${new Date().toISOString()}] Loaded ${Object.keys(fcmTokens).length} FCM tokens from disk`);
    }
    if (fs.existsSync(MEMBERS_FILE)) {
        knownMembers = JSON.parse(fs.readFileSync(MEMBERS_FILE, 'utf8'));
        // Reset online status on reboot
        Object.keys(knownMembers).forEach(id => knownMembers[id].isOnline = false);
        console.log(`[${new Date().toISOString()}] Loaded ${Object.keys(knownMembers).length} members from disk`);
    }
} catch (e) {
    console.error(`[${new Date().toISOString()}] Load error:`, e);
}

function saveMessages() {
    try {
        fs.writeFileSync(MESSAGES_FILE, JSON.stringify(messages));
    } catch (e) {
        console.error(`[${new Date().toISOString()}] Error saving messages:`, e);
    }
}

function saveTokens() {
    try {
        fs.writeFileSync(TOKENS_FILE, JSON.stringify(fcmTokens));
    } catch (e) {
        console.error(`[${new Date().toISOString()}] Error saving tokens:`, e);
    }
}

function saveMembers() {
    try {
        fs.writeFileSync(MEMBERS_FILE, JSON.stringify(knownMembers));
    } catch (e) {
        console.error(`[${new Date().toISOString()}] Error saving members:`, e);
    }
}

app.use(express.json());

// Health check
app.get('/', (req, res) => res.json({ status: 'Zehtin server running', members: Object.keys(knownMembers).length, tokens: Object.keys(fcmTokens).length }));
app.get('/zehtin', (req, res) => res.json({ status: 'Zehtin server running', members: Object.keys(knownMembers).length }));

// File endpoints
app.post('/zehtin/upload', upload.single('file'), (req, res) => {
    if (!req.file) return res.status(400).json({ error: 'No file uploaded' });
    const fileUrl = `/zehtin/files/${req.file.filename}`;
    setTimeout(() => {
        fs.unlink(req.file.path, (err) => {
            if (!err) console.log(`[${new Date().toISOString()}] Deleted expired file: ${req.file.filename}`);
        });
    }, 24 * 60 * 60 * 1000);
    res.json({ fileUrl, fileName: req.file.originalname, fileSize: `${(req.file.size / 1024).toFixed(1)} KB`, isImage: req.file.mimetype.startsWith('image/') });
});

app.get('/zehtin/files/:filename', (req, res) => {
    const filePath = path.join(__dirname, 'uploads', req.params.filename);
    if (fs.existsSync(filePath)) res.sendFile(filePath);
    else res.status(404).json({ error: 'File not found' });
});

wss.on('connection', (ws) => {
  const socketId = uuidv4();
  console.log(`[${new Date().toISOString()}] New connection: ${socketId}`);

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data);
      const deviceId = msg.deviceId || socketId;

      if (!clients.has(deviceId) && !['join', 'ping'].includes(msg.type)) {
          ws.send(JSON.stringify({ type: 'error', text: 'Not authenticated' }));
          console.log(`[${new Date().toISOString()}] Blocked unauthenticated message type=${msg.type} from ${deviceId}`);
          return;
      }

      switch (msg.type) {
        case 'join':
            if (msg.inviteCode !== INVITE_CODE) {
                ws.send(JSON.stringify({ type: 'error', text: 'Invalid code' }));
                return setTimeout(() => ws.close(), 100);
            }
            clients.set(deviceId, { ws, name: msg.name, deviceId, socketId });
            console.log(`[${new Date().toISOString()}] ${msg.name} joined (device: ${deviceId})`);

            knownMembers[deviceId] = {
                name: msg.name,
                isOnline: true,
                lastSeen: null
            };
            saveMembers();

            if (msg.fcmToken) {
                fcmTokens[deviceId] = msg.fcmToken;
                saveTokens();
                console.log(`[${new Date().toISOString()}] Received FCM token for ${msg.name} during join`);
            }

            const membersList = Object.keys(knownMembers).map(id => ({
                id,
                name: knownMembers[id].name,
                isOnline: knownMembers[id].isOnline,
                lastSeen: knownMembers[id].lastSeen
            }));

            ws.send(JSON.stringify({ type: 'joined', id: deviceId, history: messages.slice(-50), members: membersList }));
            broadcast({ type: 'member_update', member: { id: deviceId, ...knownMembers[deviceId] } }, deviceId);
            break;

        case 'update_fcm_token':
            if (msg.fcmToken) {
                fcmTokens[deviceId] = msg.fcmToken;
                saveTokens();
                console.log(`[${new Date().toISOString()}] Updated FCM token for deviceId: ${deviceId}`);
            }
            break;

        case 'message':
        case 'media':
            const client = clients.get(deviceId);
            const isMedia = msg.type === 'media';
            if (!isMedia) {
                console.log(`[${new Date().toISOString()}] MSG from ${deviceId}, client=${client?.name || 'NOT FOUND'}, text=${msg.text}`);
            }
            const chatMsg = {
                id: uuidv4(),
                senderId: deviceId,
                senderName: client?.name || 'Unknown',
                text: msg.text || '',
                time: new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
                isMedia: isMedia,
                mediaName: msg.mediaName,
                mediaSize: msg.mediaSize,
                fileUrl: msg.fileUrl || '',
                isImage: msg.isImage || false
            };
            messages.push(chatMsg);
            if (messages.length > MAX_MESSAGES) messages.shift();
            saveMessages();
            broadcastAll({ type: 'message', message: chatMsg });
            sendPushNotifications(chatMsg);
            break;

        case 'rename':
            const c = clients.get(deviceId);
            if (c) {
                console.log(`[${new Date().toISOString()}] Renamed: ${c.name} → ${msg.newName} (device: ${deviceId})`);
                c.name = msg.newName;
                knownMembers[deviceId].name = msg.newName;
                saveMembers();
                broadcastAll({ type: 'member_update', member: { id: deviceId, ...knownMembers[deviceId] } });
            }
            break;

        case 'ping':
          ws.send(JSON.stringify({ type: 'pong' }));
          break;
      }
    } catch (e) { console.error('WS Error:', e); }
  });

  ws.on('close', () => {
      let leftDeviceId = null;
      let leftName = null;
      clients.forEach((c, id) => {
          if (c.socketId === socketId) {
              leftDeviceId = id;
              leftName = c.name;
          }
      });

      if (leftDeviceId) {
          console.log(`[${new Date().toISOString()}] ${leftName} disconnected`);
          setTimeout(() => {
              const current = clients.get(leftDeviceId);
              if (!current || current.socketId === socketId) {
                  clients.delete(leftDeviceId);
                  if (knownMembers[leftDeviceId]) {
                      knownMembers[leftDeviceId].isOnline = false;
                      knownMembers[leftDeviceId].lastSeen = Date.now();
                      saveMembers();
                      broadcastAll({ type: 'member_update', member: { id: leftDeviceId, ...knownMembers[leftDeviceId] } });
                      console.log(`[${new Date().toISOString()}] ${leftName} marked offline after timeout`);
                  }
              }
          }, OFFLINE_TIMEOUT);
      }
  });
});

async function sendPushNotifications(message) {
    if (!admin.apps.length) return;

    const tokens = [];
    const recipients = [];

    for (const [deviceId, token] of Object.entries(fcmTokens)) {
        const client = clients.get(deviceId);
        // Send push if user is not in the map OR if their websocket is currently closed/broken
        if (!client || client.ws.readyState !== 1) {
            tokens.push(token);
            recipients.push(deviceId);
        }
    }

    if (tokens.length === 0) return;

    try {
        const response = await admin.messaging().sendEachForMulticast({
            tokens: tokens,
            notification: {
                title: message.senderName,
                body: message.isMedia ? "Sent a file" : message.text
            },
            data: {
                senderId: message.senderId,
                senderName: message.senderName,
                text: message.text || '',
                isMedia: message.isMedia.toString(),
                mediaName: message.mediaName || '',
                fileUrl: message.fileUrl || '',
                isImage: (message.isImage || false).toString()
            },
            android: {
                priority: 'high',
                notification: {
                    sound: 'default'
                }
            }
        });
        console.log(`[${new Date().toISOString()}] FCM: Sent to [${recipients.join(', ')}]. Results: Success=${response.successCount}, Failure=${response.failureCount}`);
    } catch (error) {
        console.error('FCM Error:', error);
    }
}

function broadcast(data, excludeId) {
  const json = JSON.stringify(data);
  clients.forEach((c, id) => {
    if (id !== excludeId && c.ws.readyState === 1) c.ws.send(json);
  });
}

function broadcastAll(data) {
  const json = JSON.stringify(data);
  clients.forEach((c) => {
    if (c.ws.readyState === 1) c.ws.send(json);
  });
}

server.listen(PORT, () => console.log(`[${new Date().toISOString()}] Zehtin server running on port ${PORT}`));
