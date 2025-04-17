# Indica la versión mínima de CMake
cmake_minimum_required(VERSION 4.0.1)

# Nombre de tu “proyecto” nativo
project("mamitas_registration")

# 1) Busca SimpleITK (asume que compilaste SimpleElastix antes y lo pusiste
#    en un lugar al que CMake pueda llegar, o bien lo instalaste en tu sistema)
find_package(SimpleITK REQUIRED)
include(${SimpleITK_USE_FILE})

# 2) Define la librería JNI que vas a compilar
add_library(registration-lib
            SHARED
            registration_jni.cpp)

# 3) Apunta a los headers de SimpleITK
target_include_directories(registration-lib PRIVATE
    ${SimpleITK_INCLUDE_DIRS})

# 4) Enlaza con las librerías de SimpleITK/SimpleElastix
target_link_libraries(registration-lib
    ${SimpleITK_LIBRARIES}
    log)   # log es para usar __android_log_print si lo necesitas