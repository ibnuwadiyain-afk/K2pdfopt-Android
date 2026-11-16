#ifndef K2PDFOPT_JNI_H
#define K2PDFOPT_JNI_H

#include <jni.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    K2_STATE_IDLE = 0,
    K2_STATE_RUNNING = 1,
    K2_STATE_PAUSED = 2,
    K2_STATE_CANCELLED = 3,
    K2_STATE_COMPLETED = 4,
    K2_STATE_FAILED = 5
} K2JobState;

typedef struct {
    int page_number;       /* 1-based page index */
    int block_type;        /* 0=TEXT, 1=TABLE, 2=FIGURE, 3=EQUATION, 4=CAPTION, 5=FOOTNOTE, 6=HEADER, 7=FOOTER, 8=COLUMN, 9=CROP */
    float left_norm;       /* [0.0 .. 1.0] normalized page coordinates */
    float top_norm;
    float right_norm;
    float bottom_norm;
    int reading_order;
    int script_direction;  /* 0=LTR, 1=RTL, 2=VERTICAL_RL, 3=VERTICAL_LR */
    int protect_policy;    /* 0=REFLOW, 1=SCALE_TO_FIT, 2=KEEP_AS_IMAGE */
    char lang_override[64];
} K2RegionOverride;

typedef struct {
    /* Device geometry */
    int dst_width_px;
    int dst_height_px;
    int dst_dpi;
    int src_dpi;
    double dst_user_width;
    double dst_user_height;
    char unit_code[8];     /* in, cm, mm, pt, px */

    /* Conversion mode & layout */
    char mode[24];         /* fw, 2col, default (reflow), copy, crop, tm, concat */
    int max_columns;       /* -col 1, 2, 4 */
    double col_gap_min;    /* -cg */
    double col_gap_range;  /* -cgr */
    double ch_height;      /* -ch */
    double crgh;           /* -crgh */
    double word_spacing;   /* -ws */
    int wrap_text;         /* -wrap 0/1 */
    int hyphens;           /* -hy 0/1 */
    int justify;           /* -j -1=auto, 0=left, 1=center, 2=right, 3=full */
    int r2l;               /* -r2l 0=LTR, 1=RTL */
    int vertical_cjk;      /* vertical text reading mode */

    /* Margins & Cropping */
    double auto_crop_tol;  /* -ac */
    double margin_left;    /* -ml */
    double margin_top;     /* -mt */
    double margin_right;   /* -mr */
    double margin_bottom;  /* -mb */
    double odd_margin_left;
    double odd_margin_right;
    double even_margin_left;
    double even_margin_right;
    double dst_margin_left;   /* -oml */
    double dst_margin_top;    /* -omt */
    double dst_margin_right;  /* -omr */
    double dst_margin_bottom; /* -omb */

    /* Image adjustments & Scan cleanup */
    double contrast;       /* -c */
    double gamma;          /* -g */
    int sharpen;           /* -sh 0/1 */
    int dither;            /* -d 0/1 */
    int bits_per_pixel;    /* -bpc 1, 2, 4, 8 */
    int negate_colors;     /* -neg 0=off, 1=all, 2=text-only */
    int auto_straighten;   /* -as (deskew) */
    int despeckle;         /* Leptonica despeckle */
    int whiten_background; /* Leptonica background normalization */
    int remove_shadows;    /* Sauvola illumination flattening */
    int rotate_deg;        /* 0, 90, 180, 270, -1=auto */

    /* Page ranges & TOC */
    char page_range[128];  /* -p e.g. 1-10,12,15-20 */
    int preserve_toc;      /* -toc 0/1 */
    int native_pdf;        /* -n 0/1 */
    int break_pages;       /* -bp 0/1 */

    /* OCR & Multilingual */
    int ocr_enabled;       /* -ocr t (Tesseract) / m (MuPDF text layer) / 0 (off) */
    char ocr_langs[256];   /* -ocrlang e.g. eng+ara+fra */
    char tessdata_dir[512];
    int ocr_threads;       /* -ocrmt */
    int ocr_quality_mode;  /* 0=fast, 1=best */

    /* Extensions */
    int footnote_mode;     /* 0=keep, 1=end_of_paragraph, 2=end_of_section */
    int table_protection;  /* 0=none, 1=scale_to_fit, 2=keep_as_image */
    int equation_inline_image; /* 1=preserve inline equations as high-DPI bitmap */
    int target_size_kb;    /* 0=unlimited, >0=iterative DPI/BPC tuning */
    char raw_cli_flags[1024];
} K2AndroidSettings;

void k2android_settings_init_defaults(K2AndroidSettings *s);
int k2android_parse_cli_string(K2AndroidSettings *s, const char *cli_args, char *err_buf, int err_buf_len);
int k2android_format_cli_string(const K2AndroidSettings *s, char *out_buf, int out_buf_len);

#ifdef __cplusplus
}
#endif

#endif /* K2PDFOPT_JNI_H */
