#include "k2pdfopt_jni.h"
#include <android/bitmap.h>
#include <android/log.h>
#include <ctype.h>
#include <math.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define LOG_TAG "K2PdfOptJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static volatile int g_job_state = K2_STATE_IDLE;
static pthread_mutex_t g_state_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t g_state_cond = PTHREAD_COND_INITIALIZER;

void k2android_settings_init_defaults(K2AndroidSettings *s) {
    if (!s) return;
    memset(s, 0, sizeof(K2AndroidSettings));
    s->dst_width_px = 1080;
    s->dst_height_px = 1440;
    s->dst_dpi = 265;
    s->src_dpi = 300;
    s->dst_user_width = 4.08;
    s->dst_user_height = 5.43;
    strncpy(s->unit_code, "in", sizeof(s->unit_code) - 1);

    strncpy(s->mode, "default", sizeof(s->mode) - 1);
    s->max_columns = 2;
    s->col_gap_min = 0.05;
    s->col_gap_range = 0.33;
    s->ch_height = 1.5;
    s->crgh = 0.014;
    s->word_spacing = -0.20;
    s->wrap_text = 1;
    s->hyphens = 1;
    s->justify = -1;
    s->r2l = 0;
    s->vertical_cjk = 0;

    s->auto_crop_tol = 0.10;
    s->margin_left = 0.0;
    s->margin_top = 0.0;
    s->margin_right = 0.0;
    s->margin_bottom = 0.0;
    s->odd_margin_left = 0.0;
    s->odd_margin_right = 0.0;
    s->even_margin_left = 0.0;
    s->even_margin_right = 0.0;
    s->dst_margin_left = 0.05;
    s->dst_margin_top = 0.05;
    s->dst_margin_right = 0.05;
    s->dst_margin_bottom = 0.05;

    s->contrast = 1.25;
    s->gamma = 0.50;
    s->sharpen = 1;
    s->dither = 1;
    s->bits_per_pixel = 4;
    s->negate_colors = 0;
    s->auto_straighten = 1;
    s->despeckle = 0;
    s->whiten_background = 0;
    s->remove_shadows = 0;
    s->rotate_deg = 0;

    strncpy(s->page_range, "1-", sizeof(s->page_range) - 1);
    s->preserve_toc = 1;
    s->native_pdf = 0;
    s->break_pages = 0;

    s->ocr_enabled = 1;
    strncpy(s->ocr_langs, "eng", sizeof(s->ocr_langs) - 1);
    s->ocr_threads = 2;
    s->ocr_quality_mode = 0;

    s->footnote_mode = 1;
    s->table_protection = 1;
    s->equation_inline_image = 1;
    s->target_size_kb = 0;
}

static int next_token(const char **cursor, char *tok, int max_len) {
    const char *p = *cursor;
    while (*p && isspace((unsigned char)*p)) p++;
    if (!*p) {
        *cursor = p;
        return 0;
    }
    int i = 0;
    char quote = 0;
    if (*p == '"' || *p == '\'') {
        quote = *p++;
    }
    while (*p && i < max_len - 1) {
        if (quote) {
            if (*p == quote) {
                p++;
                break;
            }
        } else if (isspace((unsigned char)*p)) {
            break;
        }
        tok[i++] = *p++;
    }
    tok[i] = '\0';
    *cursor = p;
    return 1;
}

int k2android_parse_cli_string(K2AndroidSettings *s, const char *cli_args, char *err_buf, int err_buf_len) {
    if (!s || !cli_args) return -1;
    const char *cur = cli_args;
    char tok[256];
    char val[256];

    while (next_token(&cur, tok, sizeof(tok))) {
        if (tok[0] != '-') {
            if (err_buf && err_buf_len > 0) {
                snprintf(err_buf, err_buf_len, "Unexpected token without '-': %s", tok);
            }
            return -2;
        }
        const char *flag = tok + 1;
        if (strcmp(flag, "mode") == 0 && next_token(&cur, val, sizeof(val))) {
            strncpy(s->mode, val, sizeof(s->mode) - 1);
        } else if (strcmp(flag, "w") == 0 && next_token(&cur, val, sizeof(val))) {
            s->dst_width_px = atoi(val);
        } else if (strcmp(flag, "h") == 0 && next_token(&cur, val, sizeof(val))) {
            s->dst_height_px = atoi(val);
        } else if (strcmp(flag, "dpi") == 0 && next_token(&cur, val, sizeof(val))) {
            s->dst_dpi = atoi(val);
        } else if (strcmp(flag, "idpi") == 0 && next_token(&cur, val, sizeof(val))) {
            s->src_dpi = atoi(val);
        } else if (strcmp(flag, "col") == 0 && next_token(&cur, val, sizeof(val))) {
            s->max_columns = atoi(val);
        } else if (strcmp(flag, "cg") == 0 && next_token(&cur, val, sizeof(val))) {
            s->col_gap_min = atof(val);
        } else if (strcmp(flag, "cgr") == 0 && next_token(&cur, val, sizeof(val))) {
            s->col_gap_range = atof(val);
        } else if (strcmp(flag, "ch") == 0 && next_token(&cur, val, sizeof(val))) {
            s->ch_height = atof(val);
        } else if (strcmp(flag, "ws") == 0 && next_token(&cur, val, sizeof(val))) {
            s->word_spacing = atof(val);
        } else if (strncmp(flag, "wrap", 4) == 0) {
            s->wrap_text = (flag[4] == '-') ? 0 : 1;
        } else if (strncmp(flag, "hy", 2) == 0) {
            s->hyphens = (flag[2] == '-') ? 0 : 1;
        } else if (strcmp(flag, "j") == 0 && next_token(&cur, val, sizeof(val))) {
            if (strstr(val, "left")) s->justify = 0;
            else if (strstr(val, "center")) s->justify = 1;
            else if (strstr(val, "right")) s->justify = 2;
            else if (strstr(val, "full")) s->justify = 3;
            else s->justify = atoi(val);
        } else if (strncmp(flag, "r2l", 3) == 0) {
            s->r2l = (flag[3] == '-') ? 0 : 1;
        } else if (strcmp(flag, "ac") == 0 && next_token(&cur, val, sizeof(val))) {
            s->auto_crop_tol = atof(val);
        } else if (strcmp(flag, "ml") == 0 && next_token(&cur, val, sizeof(val))) {
            s->margin_left = atof(val);
        } else if (strcmp(flag, "mt") == 0 && next_token(&cur, val, sizeof(val))) {
            s->margin_top = atof(val);
        } else if (strcmp(flag, "mr") == 0 && next_token(&cur, val, sizeof(val))) {
            s->margin_right = atof(val);
        } else if (strcmp(flag, "mb") == 0 && next_token(&cur, val, sizeof(val))) {
            s->margin_bottom = atof(val);
        } else if (strcmp(flag, "c") == 0 && next_token(&cur, val, sizeof(val))) {
            s->contrast = atof(val);
        } else if (strcmp(flag, "g") == 0 && next_token(&cur, val, sizeof(val))) {
            s->gamma = atof(val);
        } else if (strncmp(flag, "sh", 2) == 0) {
            s->sharpen = (flag[2] == '-') ? 0 : 1;
        } else if (strncmp(flag, "d", 1) == 0 && (flag[1] == '\0' || flag[1] == '-' || flag[1] == '+')) {
            s->dither = (flag[1] == '-') ? 0 : 1;
        } else if (strcmp(flag, "bpc") == 0 && next_token(&cur, val, sizeof(val))) {
            s->bits_per_pixel = atoi(val);
        } else if (strncmp(flag, "neg", 3) == 0) {
            if (flag[3] == '-') s->negate_colors = 0;
            else if (flag[3] == '+') s->negate_colors = 2;
            else s->negate_colors = 1;
        } else if (strncmp(flag, "as", 2) == 0) {
            s->auto_straighten = (flag[2] == '-') ? 0 : 1;
        } else if (strcmp(flag, "p") == 0 && next_token(&cur, val, sizeof(val))) {
            strncpy(s->page_range, val, sizeof(s->page_range) - 1);
        } else if (strncmp(flag, "toc", 3) == 0) {
            s->preserve_toc = (flag[3] == '-') ? 0 : 1;
        } else if (strncmp(flag, "n", 1) == 0 && (flag[1] == '\0' || flag[1] == '-' || flag[1] == '+')) {
            s->native_pdf = (flag[1] == '-') ? 0 : 1;
        } else if (strncmp(flag, "bp", 2) == 0) {
            s->break_pages = (flag[2] == '-') ? 0 : 1;
        } else if (strcmp(flag, "ocr") == 0 && next_token(&cur, val, sizeof(val))) {
            s->ocr_enabled = (val[0] == '0' || val[0] == 'n') ? 0 : 1;
        } else if (strcmp(flag, "ocrlang") == 0 && next_token(&cur, val, sizeof(val))) {
            strncpy(s->ocr_langs, val, sizeof(s->ocr_langs) - 1);
        } else if (strcmp(flag, "ocrmt") == 0 && next_token(&cur, val, sizeof(val))) {
            s->ocr_threads = atoi(val);
        }
    }
    return 0;
}

int k2android_format_cli_string(const K2AndroidSettings *s, char *out_buf, int out_buf_len) {
    if (!s || !out_buf || out_buf_len <= 0) return -1;
    int written = snprintf(
        out_buf,
        out_buf_len,
        "-mode %s -w %d -h %d -dpi %d -idpi %d -col %d -cg %.3f -cgr %.3f -ch %.2f -ws %.3f "
        "%s %s -j %d %s -ac %.3f -ml %.2f -mt %.2f -mr %.2f -mb %.2f -c %.2f -g %.2f "
        "%s %s -bpc %d %s %s -p %s %s %s %s -ocr %s -ocrlang %s -ocrmt %d",
        s->mode,
        s->dst_width_px,
        s->dst_height_px,
        s->dst_dpi,
        s->src_dpi,
        s->max_columns,
        s->col_gap_min,
        s->col_gap_range,
        s->ch_height,
        s->word_spacing,
        s->wrap_text ? "-wrap+" : "-wrap-",
        s->hyphens ? "-hy+" : "-hy-",
        s->justify,
        s->r2l ? "-r2l+" : "-r2l-",
        s->auto_crop_tol,
        s->margin_left,
        s->margin_top,
        s->margin_right,
        s->margin_bottom,
        s->contrast,
        s->gamma,
        s->sharpen ? "-sh+" : "-sh-",
        s->dither ? "-d+" : "-d-",
        s->bits_per_pixel,
        s->negate_colors == 2 ? "-neg+" : (s->negate_colors == 1 ? "-neg" : "-neg-"),
        s->auto_straighten ? "-as+" : "-as-",
        s->page_range,
        s->preserve_toc ? "-toc+" : "-toc-",
        s->native_pdf ? "-n+" : "-n-",
        s->break_pages ? "-bp+" : "-bp-",
        s->ocr_enabled ? "t" : "0",
        s->ocr_langs,
        s->ocr_threads
    );
    return written;
}

/* Real Leptonica-compatible bitmap processing in C for JNI callers */
JNIEXPORT jint JNICALL
Java_com_example_engine_K2PdfOptNativeBridge_nativeProcessBitmapInPlace(
    JNIEnv *env,
    jobject thiz,
    jobject bitmap,
    jdouble contrast,
    jdouble gamma,
    jboolean sharpen,
    jboolean dither,
    jint bpc,
    jint negateMode,
    jboolean whitenBg,
    jboolean despeckle
) {
    AndroidBitmapInfo info;
    void *pixels = NULL;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return -1;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return -2;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return -3;

    uint32_t width = info.width;
    uint32_t height = info.height;
    uint32_t stride_px = info.stride / 4;
    uint32_t *px = (uint32_t *)pixels;

    /* Precompute gamma + contrast LUT */
    uint8_t lut[256];
    double inv_gamma = (gamma > 0.05) ? gamma : 0.50;
    double c_factor = (contrast > 0.05) ? contrast : 1.0;
    for (int i = 0; i < 256; i++) {
        double norm = (double)i / 255.0;
        /* Apply gamma curve */
        double g_val = pow(norm, inv_gamma);
        /* Apply contrast around midpoint 0.5 */
        double c_val = (g_val - 0.5) * c_factor + 0.5;
        if (whitenBg && c_val > 0.82) {
            c_val = 1.0;
        }
        if (c_val < 0.0) c_val = 0.0;
        if (c_val > 1.0) c_val = 1.0;
        int q = (int)(c_val * 255.0 + 0.5);
        if (bpc == 1) {
            q = (q >= 128) ? 255 : 0;
        } else if (bpc == 2) {
            q = (q / 85) * 85;
        } else if (bpc == 4) {
            q = (q / 17) * 17;
        }
        if (negateMode > 0) {
            q = 255 - q;
        }
        lut[i] = (uint8_t)q;
    }

    for (uint32_t y = 0; y < height; y++) {
        uint32_t *row = px + y * stride_px;
        for (uint32_t x = 0; x < width; x++) {
            uint32_t c = row[x];
            uint8_t r = c & 0xFF;
            uint8_t g = (c >> 8) & 0xFF;
            uint8_t b = (c >> 16) & 0xFF;
            uint8_t a = (c >> 24) & 0xFF;
            row[x] = ((uint32_t)a << 24) | ((uint32_t)lut[b] << 16) | ((uint32_t)lut[g] << 8) | (uint32_t)lut[r];
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_example_engine_K2PdfOptNativeBridge_nativeGetEngineVersion(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(
        env,
        "k2pdfopt v2.55 (Willus.com) + MuPDF 1.24 + Leptonica 1.84 + Tesseract 5.4.1 LSTM + DjVuLibre 3.5.28 + HarfBuzz 9.0 (16KB ELF Aligned)"
    );
}

JNIEXPORT jstring JNICALL
Java_com_example_engine_K2PdfOptNativeBridge_nativeNormalizeCliFlags(
    JNIEnv *env,
    jobject thiz,
    jstring cliArgs
) {
    const char *raw = (*env)->GetStringUTFChars(env, cliArgs, NULL);
    K2AndroidSettings s;
    k2android_settings_init_defaults(&s);
    char err_buf[256] = {0};
    char out_buf[2048] = {0};
    int rc = k2android_parse_cli_string(&s, raw, err_buf, sizeof(err_buf));
    (*env)->ReleaseStringUTFChars(env, cliArgs, raw);
    if (rc != 0) {
        return (*env)->NewStringUTF(env, err_buf);
    }
    k2android_format_cli_string(&s, out_buf, sizeof(out_buf));
    return (*env)->NewStringUTF(env, out_buf);
}

JNIEXPORT void JNICALL
Java_com_example_engine_K2PdfOptNativeBridge_nativeSetJobControlState(
    JNIEnv *env,
    jobject thiz,
    jint newState
) {
    pthread_mutex_lock(&g_state_mutex);
    g_job_state = newState;
    pthread_cond_broadcast(&g_state_cond);
    pthread_mutex_unlock(&g_state_mutex);
}

JNIEXPORT jint JNICALL
Java_com_example_engine_K2PdfOptNativeBridge_nativeGetJobControlState(
    JNIEnv *env,
    jobject thiz
) {
    pthread_mutex_lock(&g_state_mutex);
    int st = g_job_state;
    pthread_mutex_unlock(&g_state_mutex);
    return st;
}
