const videoElement = document.getElementById("localVideo");
const ws = new WebSocket("ws://localhost:8080/webrtc");  // WebSocket 服务器地址
let peerConnection;
const config = { iceServers: [{ urls: "stun:stun.l.google.com:19302" }] };

async function startStream() {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    videoElement.srcObject = stream;

    peerConnection = new RTCPeerConnection(config);
    stream.getTracks().forEach(track => peerConnection.addTrack(track, stream));

    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            ws.send(JSON.stringify({ type: "candidate", candidate: event.candidate }));
        }
    };

    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    ws.send(JSON.stringify({ type: "offer", sdp: offer.sdp }));
}

ws.onmessage = async (message) => {
    const data = JSON.parse(message.data);
    if (data.type === "answer") {
        await peerConnection.setRemoteDescription(new RTCSessionDescription({ type: "answer", sdp: data.sdp }));
    } else if (data.type === "candidate") {
        await peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate));
    }
};

startStream();
