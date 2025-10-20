# Socket.IO Client Integration Guide

This guide explains how to connect to the Socket.IO server and receive real-time notifications using `socket.emit()` and `socket.on()`.

## Server Configuration

- **Socket.IO Server URL**: `http://localhost:9093` (default)
- **Protocol**: Socket.IO v2.x compatible
- **Transport**: WebSocket with polling fallback

## JavaScript/TypeScript Client Example

### Installation

```bash
npm install socket.io-client
```

### Basic Connection

```javascript
import { io } from 'socket.io-client';

// Connect to Socket.IO server
const socket = io('http://localhost:9093', {
  transports: ['websocket', 'polling'],
  reconnection: true,
  reconnectionDelay: 1000,
  reconnectionAttempts: 5
});

// Connection event handlers
socket.on('connect', () => {
  console.log('✅ Connected to Socket.IO server:', socket.id);
  
  // Join user-specific room to receive notifications
  const userId = '123'; // Replace with actual user ID
  socket.emit('join-room', userId, (response, roomName) => {
    console.log('Joined room:', response, roomName);
  });
});

socket.on('disconnect', (reason) => {
  console.log('❌ Disconnected:', reason);
});

socket.on('connect_error', (error) => {
  console.error('Connection error:', error);
});

// Listen for notifications
socket.on('notification', (notification) => {
  console.log('🔔 Received notification:', notification);
  
  // Notification structure:
  // {
  //   id: number,
  //   message: string,
  //   type: 'FAULT_LIKED' | 'FAULT_DISLIKED',
  //   faultId: number,
  //   faultTitle: string,
  //   actionBy: string,
  //   read: boolean,
  //   createdAt: string
  // }
  
  // Handle the notification (e.g., show toast, update UI)
  showNotificationToast(notification);
});
```

### React Hook Example

```typescript
import { useEffect, useState } from 'react';
import { io, Socket } from 'socket.io-client';

interface Notification {
  id: number;
  message: string;
  type: 'FAULT_LIKED' | 'FAULT_DISLIKED';
  faultId: number;
  faultTitle: string;
  actionBy: string;
  read: boolean;
  createdAt: string;
}

export function useSocketIO(userId: string | null) {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    if (!userId) return;

    // Create socket connection
    const newSocket = io('http://localhost:9093', {
      transports: ['websocket', 'polling'],
      reconnection: true,
    });

    // Connection handlers
    newSocket.on('connect', () => {
      console.log('✅ Socket.IO connected');
      setIsConnected(true);
      
      // Join user room
      newSocket.emit('join-room', userId);
    });

    newSocket.on('disconnect', () => {
      console.log('❌ Socket.IO disconnected');
      setIsConnected(false);
    });

    // Listen for notifications
    newSocket.on('notification', (notification: Notification) => {
      console.log('🔔 New notification:', notification);
      setNotifications(prev => [notification, ...prev]);
    });

    setSocket(newSocket);

    // Cleanup on unmount
    return () => {
      newSocket.emit('leave-room', userId);
      newSocket.disconnect();
    };
  }, [userId]);

  return { socket, notifications, isConnected };
}

// Usage in component:
function App() {
  const userId = '123'; // Get from auth context
  const { notifications, isConnected } = useSocketIO(userId);

  return (
    <div>
      <p>Status: {isConnected ? '🟢 Connected' : '🔴 Disconnected'}</p>
      <h2>Notifications ({notifications.length})</h2>
      {notifications.map(notif => (
        <div key={notif.id}>
          <p>{notif.message}</p>
          <small>{new Date(notif.createdAt).toLocaleString()}</small>
        </div>
      ))}
    </div>
  );
}
```

### Vue.js Example

```vue
<template>
  <div>
    <p>Status: {{ isConnected ? '🟢 Connected' : '🔴 Disconnected' }}</p>
    <h2>Notifications ({{ notifications.length }})</h2>
    <div v-for="notif in notifications" :key="notif.id">
      <p>{{ notif.message }}</p>
      <small>{{ formatDate(notif.createdAt) }}</small>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { io } from 'socket.io-client';

const socket = ref(null);
const notifications = ref([]);
const isConnected = ref(false);
const userId = '123'; // Get from auth store

onMounted(() => {
  socket.value = io('http://localhost:9093', {
    transports: ['websocket', 'polling'],
  });

  socket.value.on('connect', () => {
    isConnected.value = true;
    socket.value.emit('join-room', userId);
  });

  socket.value.on('disconnect', () => {
    isConnected.value = false;
  });

  socket.value.on('notification', (notification) => {
    notifications.value.unshift(notification);
  });
});

onUnmounted(() => {
  if (socket.value) {
    socket.value.emit('leave-room', userId);
    socket.value.disconnect();
  }
});

function formatDate(date) {
  return new Date(date).toLocaleString();
}
</script>
```

## Available Events

### Client → Server

| Event | Data | Description |
|-------|------|-------------|
| `join-room` | `userId: string` | Join a user-specific room to receive notifications |
| `leave-room` | `userId: string` | Leave the user's notification room |

### Server → Client

| Event | Data | Description |
|-------|------|-------------|
| `notification` | `NotificationResponse` | Receives a new notification |
| `connect` | - | Socket connected successfully |
| `disconnect` | `reason: string` | Socket disconnected |
| `connect_error` | `error: Error` | Connection error occurred |

## Environment Variables

Add to your `.env` file:

```env
# Socket.IO Server Configuration
SOCKETIO_HOST=0.0.0.0
SOCKETIO_PORT=9093
```

## Testing the Connection

```javascript
// Simple test script
const { io } = require('socket.io-client');

const socket = io('http://localhost:9093');

socket.on('connect', () => {
  console.log('✅ Connected!');
  socket.emit('join-room', '123');
});

socket.on('notification', (data) => {
  console.log('📬 Notification received:', data);
});
```

## Protocol

The backend uses **Socket.IO** (port 9093) for real-time notifications, providing full compatibility with JavaScript/TypeScript clients using the familiar `socket.emit()` and `socket.on()` API.

## Troubleshooting

### Connection Issues

1. **Check server is running**: Ensure Spring Boot application is running
2. **Verify port**: Default is 9093, check `application.yml`
3. **CORS**: Server allows all origins by default (`*`)
4. **Firewall**: Ensure port 9093 is not blocked

### Not Receiving Notifications

1. **Verify room join**: Ensure `join-room` event was emitted with correct userId
2. **Check userId**: Must match the userId used when sending notifications
3. **Check logs**: Server logs will show notification delivery attempts

## Production Considerations

1. **Change CORS settings** in `SocketIOConfig.java` to restrict origins
2. **Use environment variables** for host/port configuration
3. **Enable authentication** by adding custom handshake authorization
4. **Use secure connection** (WSS) with SSL certificates
5. **Consider load balancing** with sticky sessions for Socket.IO
