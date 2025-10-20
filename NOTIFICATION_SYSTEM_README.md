# Kafka Notification System - Implementation Guide

## Overview

This document describes the Kafka-based notification system implemented for the Leader's Fault application. The system sends real-time notifications to users when someone agrees (likes) or disagrees (dislikes) with faults they've posted.

## Architecture

### Components

1. **Kafka Broker** - Message broker for asynchronous event processing
2. **Notification Entity** - Database storage for notifications
3. **Kafka Producer** - Publishes notification events
4. **Kafka Consumer** - Consumes events and creates notifications
5. **WebSocket** - Real-time push notifications to connected clients
6. **REST API** - Notification management endpoints

### Flow

```
User likes/dislikes fault
    ↓
FaultController publishes event to Kafka
    ↓
Kafka Consumer receives event
    ↓
1. Saves notification to PostgreSQL
2. Sends real-time notification via WebSocket
    ↓
Frontend receives notification
```

## Setup Instructions

### 1. Start Kafka Infrastructure

Start Kafka using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- **Zookeeper** on port 2181
- **Kafka** on port 9092
- **Kafka UI** on port 8090 (http://localhost:8090)

### 2. Install Maven Dependencies

The required dependencies are already added to `pom.xml`:
- `spring-kafka`
- `spring-boot-starter-websocket`

Run:
```bash
mvn clean install
```

### 3. Environment Variables

No additional environment variables are required. Kafka is configured to use `localhost:9092` by default. To use a different Kafka server, set:

```
KAFKA_BOOTSTRAP_SERVERS=your-kafka-server:9092
```

### 4. Database Schema

The notification table will be automatically created by Hibernate with the following schema:

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    fault_id BIGINT NOT NULL,
    fault_title VARCHAR(255) NOT NULL,
    action_by VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
```

### 5. Start the Application

```bash
mvn spring-boot:run
```

## API Endpoints

### Get Notifications (Paginated)

```http
GET /api/notifications?page=0&limit=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "message": "john_doe agreed with the fault you posted",
      "type": "FAULT_LIKED",
      "faultId": 5,
      "faultTitle": "Sample Fault",
      "actionBy": "john_doe",
      "isRead": false,
      "createdAt": "2025-10-19T10:00:00"
    }
  ],
  "totalPages": 1
}
```

### Get Unread Count

```http
GET /api/notifications/unread-count
Authorization: Bearer <token>
```

**Response:**
```json
{
  "unreadCount": 5
}
```

### Mark Notification as Read

```http
PUT /api/notifications/{id}/read
Authorization: Bearer <token>
```

### Mark All Notifications as Read

```http
PUT /api/notifications/mark-all-read
Authorization: Bearer <token>
```

### Delete Notification

```http
DELETE /api/notifications/{id}
Authorization: Bearer <token>
```

## WebSocket Integration

### Frontend Connection (JavaScript Example)

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to user's notification channel
  const userId = getCurrentUserId(); // Get from your auth context
  
  stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
    const notification = JSON.parse(message.body);
    console.log('New notification:', notification);
    
    // Display notification to user
    showNotificationPopup(notification);
    
    // Update notification badge count
    updateNotificationBadge();
  });
});
```

### React Example with SockJS

```javascript
import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

function useNotifications(userId) {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
      stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
        const notification = JSON.parse(message.body);
        setNotifications(prev => [notification, ...prev]);
      });
    });

    return () => {
      if (stompClient.connected) {
        stompClient.disconnect();
      }
    };
  }, [userId]);

  return notifications;
}
```

## Features

### ✅ Implemented Features

1. **Real-time Notifications** - WebSocket push notifications
2. **Persistent Storage** - Notifications saved in PostgreSQL
3. **No Self-Notifications** - Users don't get notified for their own actions
4. **Paginated API** - Efficient notification retrieval
5. **Unread Count** - Track unread notifications
6. **Mark as Read** - Individual or bulk marking
7. **Delete Notifications** - Remove unwanted notifications
8. **Scalable Architecture** - Kafka enables horizontal scaling

### Notification Types

- `FAULT_LIKED` - When someone agrees with your fault
- `FAULT_DISLIKED` - When someone disagrees with your fault

### Message Format

- **Like:** `{username} agreed with the fault you posted`
- **Dislike:** `{username} disagreed with the fault you posted`

## Testing the System

### 1. Start Services

```bash
# Start Kafka
docker-compose up -d

# Start Spring Boot application
mvn spring-boot:run
```

### 2. Test Flow

1. **User A** creates a fault
2. **User B** likes/dislikes the fault
3. **User A** receives:
   - Database notification (persistent)
   - WebSocket notification (real-time, if connected)

### 3. Verify Kafka Messages

Open Kafka UI at http://localhost:8090 to monitor:
- Topic: `fault-notifications`
- Consumer group: `notification-consumer-group`

### 4. Test REST API

```bash
# Get notifications
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/notifications

# Get unread count
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/notifications/unread-count
```

## Monitoring

### Kafka UI

Access Kafka UI at http://localhost:8090 to:
- View topics and messages
- Monitor consumer lag
- Check consumer group status

### Application Logs

Monitor notification processing in application logs:
```
Kafka event published: NotificationType.FAULT_LIKED
Kafka event consumed: faultId=5, actionBy=john_doe
Notification saved: id=1, userId=2
WebSocket notification sent to user: 2
```

## Troubleshooting

### Kafka Connection Issues

**Problem:** Application can't connect to Kafka  
**Solution:** 
- Ensure Kafka is running: `docker ps`
- Check Kafka logs: `docker logs kafka`
- Verify port 9092 is accessible

### WebSocket Connection Issues

**Problem:** Frontend can't connect to WebSocket  
**Solution:**
- Check CORS configuration in `CorsConfig.java`
- Verify WebSocket endpoint: `ws://localhost:8080/ws`
- Check browser console for connection errors

### Notifications Not Received

**Problem:** User doesn't receive notifications  
**Solution:**
1. Check if Kafka consumer is running
2. Verify notification was created in database
3. Check user is subscribed to correct WebSocket topic
4. Ensure user is not the fault owner (no self-notifications)

## Performance Considerations

### Scaling

- **Kafka Partitions:** Increase partitions for higher throughput
- **Consumer Instances:** Run multiple consumer instances for parallel processing
- **Database Indexing:** Index on `user_id` and `created_at` columns

### Optimization

```sql
-- Recommended indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;
```

## Future Enhancements

Potential improvements:
1. Email notifications for offline users
2. Push notifications (FCM/APNs)
3. Notification preferences/settings
4. Notification grouping (multiple likes → "5 people agreed...")
5. Notification expiration/archiving
6. Rich notifications with images/actions

## Support

For issues or questions:
1. Check application logs
2. Verify Kafka is running
3. Review this documentation
4. Check Kafka UI for message flow
