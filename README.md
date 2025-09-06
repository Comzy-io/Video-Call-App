# VideoCore Android Library 

*Because who needs WhatsApp when you can build your own video calling app and pretend you're Mark Zuckerberg?*

## What is this thing?

VideoCore is an Android library that makes WebRTC video calling as easy as ordering pizza (and probably more reliable than your local pizza place's delivery time estimates). It handles all the scary WebRTC stuff so you don't have to learn what an ICE candidate is or why STUN servers exist.

## Features

- **One-to-One Video Calls**: Because group calls are just organized chaos
- **Camera Switching**: Front camera for selfies, back camera for "professional" calls
- **Mute/Unmute**: For when your cat decides to perform an opera
- **Auto Permission Handling**: We'll ask nicely for camera and mic access
- **WebSocket Signaling**: Real-time communication that actually works
- **High Quality Video**: Up to 1080p (if your internet doesn't hate you)

## Installation

### Prerequisites

Make sure your `build.gradle` (Module: app) includes VideoCore Module



### Permissions

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

*Pro tip: The library will check permissions for you, but Android still requires you to declare them. It's like telling your mom you'll clean your room - you still have to actually do it.*

## Quick Start

### 1. Initialize VideoCore

```java
// Start a video call - it's that simple!
VideoCore.start(context, "user123", "user456");
```

### 2. Get Video Views

```java
VideoCore videoCore = VideoCore.getInstance(context);

// Get local video view (your beautiful face)
View localVideoView = videoCore.getLocalVideoView();

// Get remote video view (their hopefully beautiful face)
View remoteVideoView = videoCore.getRemoteVideoView();

// Add them to your layout
yourLayout.addView(localVideoView);
yourLayout.addView(remoteVideoView);
```

### 3. Control the Call

```java
// Toggle camera (because sometimes you need the back camera)
boolean isFrontCamera = videoCore.toggleCamera();

// Toggle mute (for those "can you hear me now?" moments)
boolean isMuted = videoCore.toggleMute();

// End call (when the conversation gets awkward)
videoCore.endCall();
```

## Configuration

### Server URL

The library connects to `wss://comzy.io:8443` by default. If you're running your own signaling server (look at you, being all self-sufficient), you'll need to modify the `SIGNALING_SERVER_URL` constant in the VideoCore class.

You can find the signaling server implementation here:
👉 [Signaling Server Repository](https://github.com/Comzy-io/Video-Call-Signaling-Server)

```java
private static final String SIGNALING_SERVER_URL = "wss://your-awesome-server.com:8443";
```

### Video Quality

Default settings:
- **Resolution**: 1920x1080 (because pixels matter)
- **FPS**: 30 (smooth like butter)
- **Bitrate**: 2.5 Mbps max (adjust based on your internet speed)

Want to change these? Look for these constants in VideoCore:

```java
private static final int VIDEO_RESOLUTION_WIDTH = 1920;
private static final int VIDEO_RESOLUTION_HEIGHT = 1080;
private static final int FPS = 30;
```

## Architecture

The library follows the "make it work, then make it pretty" philosophy:

1. **WebSocket Connection**: Handles signaling with the server
2. **PeerConnection**: The WebRTC magic happens here
3. **Media Tracks**: Audio and video streams management
4. **UI Integration**: SurfaceViewRenderer for video display

## Error Handling

The library handles common errors gracefully:

- **Permission Denied**: We'll log it and cry internally
- **Camera Not Available**: Probably because another app is using it (looking at you, Instagram)
- **Network Issues**: We'll try our best, but we can't fix your WiFi
- **Peer Connection Failures**: Sometimes the internet gods are not in your favor

## Troubleshooting

### "Video call not connecting"
- Check your internet connection (yes, really)
- Make sure the signaling server is running
- Verify both users are using different userIds (you can't call yourself, that's just sad)

### "No video showing"
- Check camera permissions
- Try switching cameras
- Make sure you added the video views to your layout (rookie mistake)

### "No audio"
- Check microphone permissions
- Try toggling mute/unmute
- Check if your device is on silent (we've all been there)

### "App crashes on start"
- Make sure you have WebRTC dependency added
- Check if you're running on a real device (emulator support is... questionable)

## Use Cases

### 1. **Dating App** 💕
*"Swipe right for video calls"*
- Implement video verification for profiles
- Virtual dates when you're too lazy to go out
- Perfect for when you want to see if they actually look like their photos

### 2. **Remote Work Solutions** 💼
*"Because Zoom meetings weren't awkward enough"*
- One-on-one performance reviews
- Client consultations
- "Can you see my screen?" moments

### 3. **Telemedicine** 🏥
*"Doctor, doctor, give me the news"*
- Remote patient consultations
- Medical checkups from home
- Perfect for when you're too sick to leave the house (ironically)

### 4. **Education Platform** 📚
*"Virtual classrooms for the TikTok generation"*
- One-on-one tutoring sessions
- Language exchange programs
- When you need help with homework but your friend lives across town

### 5. **Gaming Communities** 🎮
*"Voice chat but with faces"*
- Strategy discussions with teammates
- Show off your gaming setup
- Rage quit with style (they can see your face when you lose)

### 6. **Family Video Calls** 👨‍👩‍👧‍👦
*"Grandma wants to see your face"*
- Stay connected with relatives
- Virtual family dinners
- Show off your new haircut (or lack thereof)

### 7. **Customer Support** 🎧
*"Have you tried turning it off and on again?"*
- Face-to-face technical support
- Product demonstrations
- When explaining over the phone just isn't enough

### 8. **Real Estate Virtual Tours** 🏠
*"Location, location, location (from your couch)"*
- Live property walkthroughs
- Remote home inspections
- Perfect for when you're too lazy to leave your current home

### 9. **Fitness Coaching** 💪
*"No pain, no gain (but with proper form)"*
- Personal training sessions
- Yoga classes
- When your trainer needs to see if you're actually doing the exercises

### 10. **Social Networking** 🌐
*"Facebook but with more face time"*
- Connect with new people
- Virtual coffee chats
- When texting just isn't personal enough

---

## Contributing

Found a bug? Want to add a feature? Pull requests are welcome! Just remember:

1. Test your changes (we don't want to break the internet)
2. Follow the existing code style (consistency is key)
3. Add comments (future you will thank present you)
4. Don't break existing functionality (please)

## License

This library is provided "as is" without any warranty. Use at your own risk. We're not responsible if your video calls become too addictive and you forget to go outside.

---

*Remember: With great video calling power comes great responsibility. Use it wisely, and may your connections be stable and your video quality crisp!* 

**Happy Coding!** 

*P.S. If this library helped you build the next big video calling app, we accept donations in the form of pizza and coffee. Just saying.* ☕🍕
