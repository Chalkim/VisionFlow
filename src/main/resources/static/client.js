const video = document.createElement("video");
video.autoplay = true;
document.body.appendChild(video);

navigator.mediaDevices.getUserMedia({ video: true })
    .then((stream) => {
        video.srcObject = stream;
        startStreaming(video);
    })
    .catch((error) => console.error("获取摄像头失败:", error));

const ws = new WebSocket("ws://localhost:8080/camera");

ws.onopen = () => console.log("WebSocket 连接成功");

function startStreaming(video) {
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");

    function captureFrame() {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        canvas.toBlob((blob) => {
            if (blob && ws.readyState === WebSocket.OPEN) {
                ws.send(blob);
            }
        }, "image/jpeg");

        requestAnimationFrame(captureFrame);
    }

    captureFrame();
}

ws.onclose = () => console.log("WebSocket 连接关闭");
ws.onerror = (error) => console.error("WebSocket 错误:", error);
