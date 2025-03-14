const video = document.createElement("video");
video.autoplay = true;
document.body.appendChild(video);

const canvas = document.createElement("canvas");
document.body.appendChild(canvas);
const ctx = canvas.getContext("2d");

const sentCountElement = document.createElement("div");
sentCountElement.id = "sentCount";
document.body.appendChild(sentCountElement);

const receivedCountElement = document.createElement("div");
receivedCountElement.id = "receivedCount";
document.body.appendChild(receivedCountElement);

let sentCount = 0;
let receivedCount = 0;

video.onloadedmetadata = () => {
    resizeCanvas();
    video.play();
};

window.addEventListener("resize", resizeCanvas);

function resizeCanvas() {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
}

navigator.mediaDevices.getUserMedia({ video: true })
    .then((stream) => {
        video.srcObject = stream;
        startStreaming(video);
    })
    .catch((error) => console.error("获取摄像头失败:", error));

const protocol = window.location.protocol === "https:" ? "wss" : "ws";
const ws = new WebSocket(`${protocol}://${window.location.host}/camera`);


ws.onopen = () => console.log("WebSocket 连接成功");

function startStreaming(video) {
    function captureFrame() {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        canvas.toBlob((blob) => {
            if (blob && ws.readyState === WebSocket.OPEN) {
                ws.send(blob);
                sentCount++;
                sentCountElement.textContent = `Sent blobs: ${sentCount}`;
            }
        }, "image/jpeg");
    }

    // Capture frame at 30 FPS
    setInterval(captureFrame, 1000 / 30);
}

let results = [];

ws.onmessage = (event) => {
    try {
        results = JSON.parse(event.data);
        receivedCount++;
        receivedCountElement.textContent = `Received messages: ${receivedCount}`;
        drawBoxes();
    } catch (error) {
        console.error("解析 WebSocket 数据失败:", error);
    }
};
ws.onclose = () => console.log("WebSocket 连接关闭");
ws.onerror = (error) => console.error("WebSocket 错误:", error);

const CLASS_NAMES = [
     "person",         "bicycle",    "car",           "motorcycle",    "airplane",     "bus",           "train",
     "truck",          "boat",       "traffic light", "fire hydrant",  "stop sign",    "parking meter", "bench",
     "bird",           "cat",        "dog",           "horse",         "sheep",        "cow",           "elephant",
     "bear",           "zebra",      "giraffe",       "backpack",      "umbrella",     "handbag",       "tie",
     "suitcase",       "frisbee",    "skis",          "snowboard",     "sports ball",  "kite",          "baseball bat",
     "baseball glove", "skateboard", "surfboard",     "tennis racket", "bottle",       "wine glass",    "cup",
     "fork",           "knife",      "spoon",         "bowl",          "banana",       "apple",         "sandwich",
     "orange",         "broccoli",   "carrot",        "hot dog",       "pizza",        "donut",         "cake",
     "chair",          "couch",      "potted plant",  "bed",           "dining table", "toilet",        "tv",
     "laptop",         "mouse",      "remote",        "keyboard",      "cell phone",   "microwave",     "oven",
     "toaster",        "sink",       "refrigerator",  "book",          "clock",        "vase",          "scissors",
     "teddy bear",     "hair drier", "toothbrush"
];

const COLORS = [
    [0, 114, 189],   [217, 83, 25],   [237, 177, 32],  [126, 47, 142],  [119, 172, 48],  [77, 190, 238],
    [162, 20, 47],   [76, 76, 76],    [153, 153, 153], [255, 0, 0],     [255, 128, 0],   [191, 191, 0],
    [0, 255, 0],     [0, 0, 255],     [170, 0, 255],   [85, 85, 0],     [85, 170, 0],    [85, 255, 0],
    [170, 85, 0],    [170, 170, 0],   [170, 255, 0],   [255, 85, 0],    [255, 170, 0],   [255, 255, 0],
    [0, 85, 128],    [0, 170, 128],   [0, 255, 128],   [85, 0, 128],    [85, 85, 128],   [85, 170, 128],
    [85, 255, 128],  [170, 0, 128],   [170, 85, 128],  [170, 170, 128], [170, 255, 128], [255, 0, 128],
    [255, 85, 128],  [255, 170, 128], [255, 255, 128], [0, 85, 255],    [0, 170, 255],   [0, 255, 255],
    [85, 0, 255],    [85, 85, 255],   [85, 170, 255],  [85, 255, 255],  [170, 0, 255],   [170, 85, 255],
    [170, 170, 255], [170, 255, 255], [255, 0, 255],   [255, 85, 255],  [255, 170, 255], [85, 0, 0],
    [128, 0, 0],     [170, 0, 0],     [212, 0, 0],     [255, 0, 0],     [0, 43, 0],      [0, 85, 0],
    [0, 128, 0],     [0, 170, 0],     [0, 212, 0],     [0, 255, 0],     [0, 0, 43],      [0, 0, 85],
    [0, 0, 128],     [0, 0, 170],     [0, 0, 212],     [0, 0, 255],     [0, 0, 0],       [36, 36, 36],
    [73, 73, 73],    [109, 109, 109], [146, 146, 146], [182, 182, 182], [219, 219, 219], [0, 114, 189],
    [80, 183, 189],  [128, 128, 0]
];

drawBoxes();

function drawBoxes() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 画检测框
    results.forEach(obj => {
        let { x, y, width, height } = obj.bbox;

        const input_h = 640;
        const input_w = 640;
        const image = {
            rows: canvas.height,
            cols: canvas.width
        }
        const box = {
            x, y, width, height
        }
        const ratio_h = input_h / image.rows;
        const ratio_w = input_w / image.cols;

        if (ratio_h > ratio_w)
        {
            box.x = box.x / ratio_w;
            box.y = (box.y - (input_h - ratio_w * image.rows) / 2) / ratio_w;
            box.width = box.width / ratio_w;
            box.height = box.height / ratio_w;
        }
        else
        {
            box.x = (box.x - (input_w - ratio_h * image.cols) / 2) / ratio_h;
            box.y = box.y / ratio_h;
            box.width = box.width / ratio_h;
            box.height = box.height / ratio_h;
        }

        x = box.x;
        y = box.y;
        width = box.width;
        height = box.height;

        const classIndex = obj.class;
        const score = obj.score.toFixed(2);

        // 获取颜色
        const color = COLORS[classIndex % COLORS.length];

        ctx.strokeStyle = `rgb(${color[0]}, ${color[1]}, ${color[2]})`;
        ctx.lineWidth = 3;

        // 调整框的坐标和大小，避免超出边界
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > canvas.width) width = canvas.width - x;
        if (y + height > canvas.height) height = canvas.height - y;

        // 画矩形框
        ctx.strokeRect(x, y, width, height);

        // 画标签
        ctx.fillStyle = ctx.strokeStyle;
        ctx.font = "18px Arial";
        const text = `${CLASS_NAMES[classIndex]} (${score})`;
        ctx.fillText(text, x + 5, y + 20);
    });

    requestAnimationFrame(drawBoxes);
}