const videoElement = document.getElementById("localVideo");
const ws = new WebSocket("ws://localhost:8080/webrtc");  // WebSocket 服务器地址
let peerConnection;
const config = { iceServers: [{ urls: "stun:stun.l.google.com:19302" }] };

async function startStream() {
    console.log("请求摄像头权限...");
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    console.log("摄像头权限获取成功，开始推流...");
    videoElement.srcObject = stream;

    peerConnection = new RTCPeerConnection(config);
    stream.getTracks().forEach(track => {
        peerConnection.addTrack(track, stream);
        console.log(`添加 track: ${track.kind}`);
    });

    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            console.log("发送 ICE candidate:", event.candidate);
            ws.send(JSON.stringify({ type: "candidate", candidate: event.candidate }));
        }
    };

    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    console.log("发送 SDP Offer:", offer.sdp);
    ws.send(JSON.stringify({ type: "offer", sdp: offer.sdp }));
}

ws.onmessage = async (message) => {
    const data = JSON.parse(message.data);
    if (data.type === "answer") {
        console.log("收到 SDP Answer:", data.sdp);
        await peerConnection.setRemoteDescription(new RTCSessionDescription({ type: "answer", sdp: data.sdp }));
    } else if (data.type === "candidate") {
        console.log("收到 ICE Candidate:", data.candidate);
        await peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate));
    }
};

startStream();
