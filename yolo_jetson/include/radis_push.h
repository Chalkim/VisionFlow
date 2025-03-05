#ifndef RADIS_PUSH_H
#define RADIS_PUSH_H

#include <iostream>
#include <cstdint>
#include <string>
#include <vector>

#include <opencv4/opencv2/opencv.hpp>
#include <opencv4/opencv2/highgui.hpp>
#include <opencv4/opencv2/imgproc.hpp>
#include <opencv4/opencv2/core.hpp>
#include <sstream>

#include <nlohmann/json.hpp>
#include "base64.h"
#include <sw/redis++/redis++.h>

using namespace sw::redis;
struct RedisMessage {
    std::string sessionId;          // session-id
    std::vector<uint8_t> imageData; // 图像转为二进制数据
    int inputH;                      // 图像高度
    int inputW;                      // 图像宽度

    std::string encode() const {
        // 将 imageData 转换为 Base64 编码的字符串
        return base64_encode(imageData);
    }

};

std::vector<uint8_t> imageToBytes(const cv::Mat& image);

#endif

