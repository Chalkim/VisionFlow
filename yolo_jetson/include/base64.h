#ifndef BASE64_H
#define BASE64_H

#include <vector>
#include <string>

std::string base64_encode(const std::vector<uint8_t>& data);
std::vector<uint8_t> base64_decode(const std::string& encoded);

#endif // BASE64_H
