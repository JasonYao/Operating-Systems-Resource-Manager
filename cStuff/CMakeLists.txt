cmake_minimum_required(VERSION 3.3)
project(Lab3_Bankers_Algorithm)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS}" -std=c11)

set(SOURCE_FILES resource-manager.c)
add_executable(Lab3_Bankers_Algorithm ${SOURCE_FILES})
