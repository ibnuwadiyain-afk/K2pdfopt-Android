cmake_minimum_required(VERSION 3.22.1)
project("k2pdfopt" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Mandatory 16 KB page-size alignment for Android 15+ (API 35+) on arm64-v8a and x86_64
add_link_options(
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

option(K2PDFOPT_WITH_UPSTREAM_DEPS "Compile and link full upstream k2pdfopt + MuPDF + Leptonica + Tesseract 5 + DjVuLibre + HarfBuzz + FreeType sources" OFF)

set(THIRD_PARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party")

# Collect k2pdfopt & willuslib sources if fetched into third_party/k2pdfopt
if(EXISTS "${THIRD_PARTY_DIR}/k2pdfopt/k2pdfoptlib")
    file(GLOB K2PDFOPT_LIB_SRCS "${THIRD_PARTY_DIR}/k2pdfopt/k2pdfoptlib/*.c")
    file(GLOB WILLUS_LIB_SRCS "${THIRD_PARTY_DIR}/k2pdfopt/willuslib/*.c")
    add_compile_definitions(
        HAVE_K2PDFOPT_UPSTREAM=1
        HAVE_MUPDF_LIB=1
        HAVE_LEPTONICA_LIB=1
        HAVE_TESSERACT_LIB=1
        HAVE_DJVU_LIB=1
        HAVE_GSL_LIB=0
        HAVE_HARFBUZZ_LIB=1
        HAVE_FREETYPE_LIB=1
    )
else()
    set(K2PDFOPT_LIB_SRCS "")
    set(WILLUS_LIB_SRCS "")
endif()

add_library(
    k2pdfopt
    SHARED
    k2pdfopt_jni.c
    ${K2PDFOPT_LIB_SRCS}
    ${WILLUS_LIB_SRCS}
)

target_include_directories(
    k2pdfopt
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    "${THIRD_PARTY_DIR}/k2pdfopt/k2pdfoptlib"
    "${THIRD_PARTY_DIR}/k2pdfopt/willuslib"
    "${THIRD_PARTY_DIR}/include"
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(z-lib z)
find_library(m-lib m)

set(EXTRA_NATIVE_LIBS "")
foreach(dep_lib mupdf mupdf-third leptonica tesseract djvulibre harfbuzz freetype jpeg png16)
    if(EXISTS "${THIRD_PARTY_DIR}/lib/${ANDROID_ABI}/lib${dep_lib}.a")
        list(APPEND EXTRA_NATIVE_LIBS "${THIRD_PARTY_DIR}/lib/${ANDROID_ABI}/lib${dep_lib}.a")
    endif()
endforeach()

target_link_libraries(
    k2pdfopt
    ${EXTRA_NATIVE_LIBS}
    ${log-lib}
    ${jnigraphics-lib}
    ${z-lib}
    ${m-lib}
)
