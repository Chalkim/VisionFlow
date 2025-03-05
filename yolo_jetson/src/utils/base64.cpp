#include "base64.h"
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include <vector>
#include <iostream>

// Base64 Encode
std::string base64_encode(const std::vector<uint8_t>& data) {
    BIO *bio, *b64;
    BUF_MEM *bufferPtr;

    b64 = BIO_new(BIO_f_base64());
    bio = BIO_new(BIO_s_mem());
    bio = BIO_push(b64, bio);

    // No newlines to match standard Base64 encoding
    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
    BIO_write(bio, data.data(), data.size());
    BIO_flush(bio);

    BIO_get_mem_ptr(bio, &bufferPtr);
    std::string encoded(bufferPtr->data, bufferPtr->length);

    BIO_free_all(bio);
    return encoded;
}

// Base64 Decode
std::vector<uint8_t> base64_decode(const std::string& encoded) {
    BIO *bio, *b64;
    int decodeLen = encoded.size() * 3 / 4; // Approximate decoded size
    std::vector<uint8_t> buffer(decodeLen);

    bio = BIO_new_mem_buf(encoded.data(), encoded.size());
    b64 = BIO_new(BIO_f_base64());
    bio = BIO_push(b64, bio);

    // No newlines
    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
    int len = BIO_read(bio, buffer.data(), buffer.size());
    
    BIO_free_all(bio);
    buffer.resize(len);
    return buffer;
}
