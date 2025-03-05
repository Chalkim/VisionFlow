cmake_minimum_required(VERSION 3.10)
project(yolo_infer LANGUAGES  CXX CUDA)
project(radispush LANGUAGES  CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# cuda setup for jetson
find_package(CUDA REQUIRED)
message("-- CUDA version: ${CUDA_VERSION}")
message("-- CUDA INC PATH: ${CUDA_INCLUDE_DIRS}")
message("-- CUDA LIB PATH: ${CUDA_LIBRARIES}")
set(CUDA_NVCC_FLAGS ${CUDA_NVCC_FLAGS}; -O3)

if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64")
	message("-- CUDA ${CUDA_VERSION} detected (${CMAKE_SYSTEM_PROCESSOR}), enabling SM_53 SM_62")
	set(CUDA_NVCC_FLAGS ${CUDA_NVCC_FLAGS}; -gencode arch=compute_53,code=sm_53 -gencode arch=compute_62,code=sm_62)

	if(CUDA_VERSION_MAJOR GREATER 9)
		message("-- CUDA ${CUDA_VERSION} detected (${CMAKE_SYSTEM_PROCESSOR}), enabling SM_72")
		set(CUDA_NVCC_FLAGS ${CUDA_NVCC_FLAGS}; -gencode arch=compute_72,code=sm_72)
	endif()

	if(CUDA_VERSION_MAJOR GREATER 10)
		message("-- CUDA ${CUDA_VERSION} detected (${CMAKE_SYSTEM_PROCESSOR}), enabling SM_87")
		set(CUDA_NVCC_FLAGS ${CUDA_NVCC_FLAGS}; -gencode arch=compute_87,code=sm_87)
	endif()
endif()

# openCV setup
find_package(OpenCV REQUIRED)
message("-- OpenCV version: ${OpenCV_VERSION}")

# tensorRT setup
set(TensorRT_INCLUDE_DIR /usr/include/aarch64-linux-gnu)
set(TensorRT_LIBRARY /usr/lib/aarch64-linux-gnu)
set(TensorRT_LIBRARIES nvinfer nvinfer_plugin)

# # base64 setup
# Find OpenSSL
find_package(OpenSSL REQUIRED)

# redis setup
find_path(HIREDIS_HEADER hiredis)
find_library(HIREDIS_LIB hiredis)
find_path(REDIS_PLUS_PLUS_HEADER sw)
find_library(REDIS_PLUS_PLUS_LIB redis++)


include_directories(${OpenCV_INCLUDE_DIRS} ${TensorRT_INCLUDE_DIR} ${CUDA_INCLUDE_DIRS}
					${BASE64_INCLUDE_DIR} ${REDIS_PLUS_PLUS_HEADER} ${HIREDIS_HEADER}
					${OpenSSL_INCLUDE_DIR})

# add headers
include_directories(include)
file(GLOB SOURCES "src/yolo_infer/*.cpp" "src/yolo_infer/*.cu" "src/utils/*.cpp")
file(GLOB SOURCES_radis "src/radis_test/*.cpp" "src/utils/*.cpp")

# add executable
add_executable(yolo_infer ${SOURCES})
add_executable(radispush ${SOURCES_radis})

# link libraries
target_link_libraries(yolo_infer ${OpenCV_LIBS} ${TensorRT_LIBRARIES} ${CUDA_LIBRARIES}
								 ${REDIS_PLUS_PLUS_LIB} ${HIREDIS_LIB} OpenSSL::SSL OpenSSL::Crypto)
target_link_libraries(radispush ${OpenCV_LIBS} ${CUDA_LIBRARIES} 
								${REDIS_PLUS_PLUS_LIB} ${HIREDIS_LIB} OpenSSL::SSL OpenSSL::Crypto)