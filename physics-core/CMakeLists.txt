cmake_minimum_required(VERSION 3.15)
project(physics_core CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Find Java Native Interface (JNI) headers
find_package(JNI QUIET)
if (JNI_FOUND)
    message(STATUS "Found JNI: ${JNI_INCLUDE_DIRS}")
    include_directories(${JNI_INCLUDE_DIRS})
else()
    # Fallback to direct path
    include_directories(
        /usr/lib/jvm/temurin-21-jdk-amd64/include
        /usr/lib/jvm/temurin-21-jdk-amd64/include/linux
    )
endif()

include_directories(include src)

set(CORE_SOURCES
    src/gravity.cpp
    src/Integrator.cpp
    src/PhysicsBridge.cpp
)

# 1. Standalone test executable
add_executable(simulateuz_physics
    src/main.cpp
    ${CORE_SOURCES}
)

# 2. Dynamic shared library for JNI
add_library(physics_core SHARED
    ${CORE_SOURCES}
)

set_target_properties(physics_core PROPERTIES
    OUTPUT_NAME "physics_core"
    PREFIX "lib"
)
