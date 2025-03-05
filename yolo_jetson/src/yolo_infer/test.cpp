#include <cstdio>
#include "dataloader.h"
#include "engineloader.h"
#include <thread>
#include <mutex>
#include <condition_variable>
#include <sw/redis++/redis++.h>
#include "base64.h"

using namespace sw::redis;
using Item = std::pair<std::string, std::unordered_map<std::string, std::string>>;
using ItemStream = std::vector<Item>;
//全局engine
EngineLoader engineloader;

auto redis = Redis("tcp://127.0.0.1:6379");
void redis_init(){
    try {
        redis.ping();
        std::cout << "Connected to Redis!" << std::endl;
    } catch (const sw::redis::Error &e) {
        std::cerr << "Failed to connect to Redis: " << e.what() << std::endl;
    }
}

void redis_listener() {
    std::unordered_map<std::string, std::string> data;
    std::string stream_name = "pending_data";
    while (true) {
        Item item;
        ItemStream item_stream;
        // 从 Redis 中读取数据
        redis.xrange(stream_name, "0", "+", 1, std::back_inserter(item_stream));
        if (!item_stream.empty()) {
            item = item_stream.front();
            std::string messageid = item.first;
            // 处理接收到的数据
            std::cout << "Received message id: " << messageid << std::endl;
            data = item.second;
            std::string sessionId = data["sessionId"];
            std::cout << "Received message sessionId: " << sessionId << std::endl;
            std::string imageData = data["imageData"];
            std::vector<uint8_t> imagebytes = base64_decode(imageData);
            cv::Mat img = cv::imdecode(imagebytes, cv::IMREAD_COLOR);
            if (img.empty()) {
                std::cerr << "Error: Could not decode image data" << std::endl;
                continue;
            }else{
                printf("image decode successfully\n");
            }

            engineloader.preprocess(img);
            engineloader.infer();
            std::vector<Detection> detections;
            engineloader.postprocess(detections);

            std::string results = detectionsToJson(detections);
            std::cout << "results: " << results << std::endl;
            
            std::unordered_map<std::string, std::string> fields = {
                {"sessionId", sessionId},
                {"results", results},
            };
            try {
                auto id = redis.xadd("pending_results", "*", fields.begin(), fields.end());
                std::cout << "Message added to stream with ID: " << id << std::endl;
            } catch (const sw::redis::Error &e) {
                std::cerr << "Failed to add message to stream: " << e.what() << std::endl;
            }
            // std::string inputH = data["inputH"];
            // std::string inputW = data["inputW"];
            // int inputh = std::stoi(inputH);
            // int inputw = std::stoi(inputW);
            redis.xdel(stream_name, messageid);
        }
    }    
}

int main() {
    cv::Mat img;
    std::string engine_filename = "yolo11n.trt";
    if (!engineloader.loadEngine(engine_filename)) {
        std::cerr << "Error: Could not load engine " << engine_filename << std::endl;
        return -1;
    } else{
        printf("engine load successfully\n");
    }
    redis_init();
    std::thread t1(redis_listener);
    t1.join();
    return 0;
}