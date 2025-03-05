#include "radis_push.h"


std::vector<uint8_t> imageToBytes(const cv::Mat& img) {
    std::vector<uint8_t> bytes;
    cv::imencode(".jpg", img, bytes);
    return bytes;
}

int main() {
    cv::Mat img;
    std::string image_filename = "bus.jpg";
    img = cv::imread(image_filename, cv::IMREAD_COLOR);
    if (img.empty()) {
        std::cerr << "Could not read the image: " << image_filename << std::endl;
        return 1;
    }

    auto redis = Redis("tcp://127.0.0.1:6379");

    // 测试连接
    try {
        redis.ping();
        std::cout << "Connected to Redis!" << std::endl;
    } catch (const Error &e) {
        std::cerr << "Failed to connect to Redis: " << e.what() << std::endl;
        return 1;
    }
    RedisMessage msg;
    msg.sessionId = "123456";
    msg.imageData = imageToBytes(img);
    msg.inputH = img.rows;
    msg.inputW = img.cols;

    // 将 RedisMessage 转换为 Redis Stream 的字段
    std::unordered_map<std::string, std::string> fields = {
        {"sessionId", msg.sessionId},
        {"imageData", msg.encode()},
        {"inputH", std::to_string(msg.inputH)},
        {"inputW", std::to_string(msg.inputW)}
    };

    // 将字段添加到 Redis Stream 中
    try {
        auto id = redis.xadd("my_stream", "*", fields.begin(), fields.end());
        std::cout << "Message added to stream with ID: " << id << std::endl;
    } catch (const Error &e) {
        std::cerr << "Failed to add message to stream: " << e.what() << std::endl;
    }

    return 0;

}