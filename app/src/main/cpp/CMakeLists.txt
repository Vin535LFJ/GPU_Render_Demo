cmake_minimum_required(VERSION 3.10.2)

project("render_native_demo")

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        runtime/RuntimeController.cpp)

set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF)

target_compile_features(${CMAKE_PROJECT_NAME} PRIVATE cxx_std_17)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_SOURCE_DIR})

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)
