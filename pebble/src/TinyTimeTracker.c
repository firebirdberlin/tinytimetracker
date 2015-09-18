#include <pebble.h>

#define KEY_TEMPERATURE 0
#define KEY_CONDITIONS 1
#define KEY_WORKTIME 2
#define KEY_BATTERY_PERCENT 3

#define KEY_WATCH_IS_PLUGGED 4
#define VALUE_WATCH_IS_PLUGGED 1
#define VALUE_WATCH_IS_UNPLUGGED 0

static Window *window;
static Layer *s_clock_layer;
static Layer *s_worktime_layer;
static TextLayer *text_layer;
static TextLayer *s_weather_layer;
static TextLayer *s_worktime_text_layer;
static GFont s_font;
static GFont s_time_font;
static GFont s_font_22;
static BitmapLayer *s_background_layer;
static BitmapLayer *s_icon_layer;
static GBitmap *s_background_bitmap;
static GBitmap *s_icon_bitmap = NULL;

static PropertyAnimation *s_property_animation;

static int angle_45 = TRIG_MAX_ANGLE / 8;
static int angle_90 = TRIG_MAX_ANGLE / 4;
static int angle_180 = TRIG_MAX_ANGLE / 2;
static int angle_270 = 3 * TRIG_MAX_ANGLE / 4;

int phone_battery_percent = 0;
/*\
|*| DrawArc function thanks to Cameron MacFarland (http://forums.getpebble.com/profile/12561/Cameron%20MacFarland)
\*/
static void graphics_draw_arc(GContext *ctx, GPoint center, int radius, int thickness, int start_angle, int end_angle, GColor c) {
    int32_t xmin = 65535000, xmax = -65535000, ymin = 65535000, ymax = -65535000;
    int32_t cosStart, sinStart, cosEnd, sinEnd;
    int32_t r, t;

    while (start_angle < 0) start_angle += TRIG_MAX_ANGLE;
    while (end_angle < 0) end_angle += TRIG_MAX_ANGLE;

    start_angle %= TRIG_MAX_ANGLE;
    end_angle %= TRIG_MAX_ANGLE;

    if (end_angle == 0) end_angle = TRIG_MAX_ANGLE;

    if (start_angle > end_angle) {
        graphics_draw_arc(ctx, center, radius, thickness, start_angle, TRIG_MAX_ANGLE, c);
        graphics_draw_arc(ctx, center, radius, thickness, 0, end_angle, c);
    } else {
        // Calculate bounding box for the arc to be drawn
        cosStart = cos_lookup(start_angle);
        sinStart = sin_lookup(start_angle);
        cosEnd = cos_lookup(end_angle);
        sinEnd = sin_lookup(end_angle);

        r = radius;
        // Point 1: radius & start_angle
        t = r * cosStart;
        if (t < xmin) xmin = t;
        if (t > xmax) xmax = t;
        t = r * sinStart;
        if (t < ymin) ymin = t;
        if (t > ymax) ymax = t;

        // Point 2: radius & end_angle
        t = r * cosEnd;
        if (t < xmin) xmin = t;
        if (t > xmax) xmax = t;
        t = r * sinEnd;
        if (t < ymin) ymin = t;
        if (t > ymax) ymax = t;

        r = radius - thickness;
        // Point 3: radius-thickness & start_angle
        t = r * cosStart;
        if (t < xmin) xmin = t;
        if (t > xmax) xmax = t;
        t = r * sinStart;
        if (t < ymin) ymin = t;
        if (t > ymax) ymax = t;

        // Point 4: radius-thickness & end_angle
        t = r * cosEnd;
        if (t < xmin) xmin = t;
        if (t > xmax) xmax = t;
        t = r * sinEnd;
        if (t < ymin) ymin = t;
        if (t > ymax) ymax = t;

        // Normalization
        xmin /= TRIG_MAX_RATIO;
        xmax /= TRIG_MAX_RATIO;
        ymin /= TRIG_MAX_RATIO;
        ymax /= TRIG_MAX_RATIO;

        // Corrections if arc crosses X or Y axis
        if ((start_angle < angle_90) && (end_angle > angle_90)) {
            ymax = radius;
        }

        if ((start_angle < angle_180) && (end_angle > angle_180)) {
            xmin = -radius;
        }

        if ((start_angle < angle_270) && (end_angle > angle_270)) {
            ymin = -radius;
        }

        // Slopes for the two sides of the arc
        float sslope = (float)cosStart/ (float)sinStart;
        float eslope = (float)cosEnd / (float)sinEnd;

        if (end_angle == TRIG_MAX_ANGLE) eslope = -1000000;

        int ir2 = (radius - thickness) * (radius - thickness);
        int or2 = radius * radius;

        graphics_context_set_stroke_color(ctx, c);

        for (int x = xmin; x <= xmax; x++) {
            for (int y = ymin; y <= ymax; y++)
            {
                int x2 = x * x;
                int y2 = y * y;

                if (
                    (x2 + y2 < or2 && x2 + y2 >= ir2) && (
                        (y > 0 && start_angle < angle_180 && x <= y * sslope) ||
                        (y < 0 && start_angle > angle_180 && x >= y * sslope) ||
                        (y < 0 && start_angle <= angle_180) ||
                        (y == 0 && start_angle <= angle_180 && x < 0) ||
                        (y == 0 && start_angle == 0 && x > 0)
                    ) && (
                        (y > 0 && end_angle < angle_180 && x >= y * eslope) ||
                        (y < 0 && end_angle > angle_180 && x <= y * eslope) ||
                        (y > 0 && end_angle >= angle_180) ||
                        (y == 0 && end_angle >= angle_180 && x < 0) ||
                        (y == 0 && start_angle == 0 && x > 0)
                    )
                )
                graphics_draw_pixel(ctx, GPoint(center.x+x, center.y+y));
            }
        }
    }
}


static void update_time() {
    // Get a tm structure
    time_t temp = time(NULL);
    struct tm *tick_time = localtime(&temp);

    // Create a long-lived buffer
    static char buffer[] = "00:00";

    // Write the current hours and minutes into the buffer
    if(clock_is_24h_style() == true) {
        // Use 24 hour format
        strftime(buffer, sizeof("00:00"), "%H:%M", tick_time);
    } else {
        // Use 12 hour format
        strftime(buffer, sizeof("00:00"), "%I:%M", tick_time);
    }

    // Display this time on the TextLayer
    text_layer_set_text(text_layer, buffer);
}


static void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
    update_time();
    // Get weather update every 30 minutes
    if(tick_time->tm_min % 30 == 0) {
        // Begin dictionary
        DictionaryIterator *iter;
        app_message_outbox_begin(&iter);

        // Add a key-value pair
        dict_write_uint8(iter, 0, 0);

        // Send the message!
        app_message_outbox_send();
    }
}

static void anim_stopped_handler(Animation *animation, bool finished, void *context) {
    property_animation_destroy(s_property_animation);
    s_property_animation = NULL;
}

static void trigger_slide_animation_to(Layer *layer, GRect to_frame) {
    if (s_property_animation) return;
    GRect from_frame = layer_get_frame(layer);

    if ( grect_equal(&from_frame, &to_frame) ) {
        return;
    }

    s_property_animation = property_animation_create_layer_frame(layer, &from_frame, &to_frame);
    animation_set_handlers((Animation*) s_property_animation, (AnimationHandlers) {
        .stopped = anim_stopped_handler
        }, NULL);
    animation_schedule((Animation*) s_property_animation);
}


static void worktime_layer_draw(Layer *layer, GContext *ctx) {
    GRect bounds = layer_get_bounds(layer);
    graphics_context_set_fill_color(ctx, GColorWhite);

    GSize textSize = text_layer_get_content_size(s_worktime_text_layer);
    if (textSize.w > 0) {
        graphics_fill_rect(ctx, bounds, 10, GCornersAll);
        trigger_slide_animation_to(s_clock_layer, GRect(5, 34, 134, 134));
    } else {
        trigger_slide_animation_to(s_clock_layer, GRect(5, 17, 134, 134));
    }
    layer_mark_dirty(text_layer_get_layer(s_worktime_text_layer));
}


static void clock_layer_draw(Layer *layer, GContext *ctx) {
    GRect bounds = layer_get_frame(layer);

    // Draw a black filled rectangle with sharp corners
    graphics_context_set_fill_color(ctx, GColorBlack);
    graphics_fill_rect(ctx, bounds, 0, GCornerNone);

    // Draw a white filled circle a radius of half the layer height
    graphics_context_set_fill_color(ctx, GColorWhite);
    const int16_t half_h = (bounds.size.h) / 2;
    const int16_t half_w = (bounds.size.w) / 2;
    GPoint center = {half_w, half_h};
    graphics_fill_circle(ctx, center, half_h - 10);

    BatteryChargeState charge_state = battery_state_service_peek();
    int battery_percent = charge_state.charge_percent;
    if (battery_percent > 0) {
        int angle = angle_180 + angle_180 * battery_percent/100;
        graphics_draw_arc(ctx, center, half_h - 3, 3, angle_180, angle, GColorWhite);
    }
    if (phone_battery_percent > 0) {
        int angle = angle_180 * phone_battery_percent/100;
        graphics_draw_arc(ctx, center, half_h - 3, 3, 0, angle, GColorWhite);
    }
}


static void window_load(Window *window) {
    // Create GBitmap, then set to created BitmapLayer
    s_background_bitmap = gbitmap_create_with_resource(RESOURCE_ID_IMAGE_BACKGROUND);
    s_background_layer = bitmap_layer_create(GRect(0, 0, 144, 168));
    bitmap_layer_set_bitmap(s_background_layer, s_background_bitmap);
    layer_add_child(window_get_root_layer(window), bitmap_layer_get_layer(s_background_layer));

    s_font         = fonts_load_custom_font(resource_get_handle(RESOURCE_ID_FONT_AUDIMAT_MONO_BOLD_28));
    s_time_font    = fonts_load_custom_font(resource_get_handle(RESOURCE_ID_FONT_AUDIMAT_MONO_BOLD_40));
    s_font_22      = fonts_load_custom_font(resource_get_handle(RESOURCE_ID_FONT_AUDIMAT_MONO_22));

    s_clock_layer = layer_create(GRect(5, 17, 134, 134));
    layer_set_update_proc(s_clock_layer, clock_layer_draw);

    s_worktime_layer = layer_create(GRect(22, 0, 100, 30));
    layer_set_update_proc(s_worktime_layer, worktime_layer_draw);

    // Add it as a child layer to the Window's root layer
    Layer *rootLayer = window_get_root_layer(window);
    layer_add_child(rootLayer, s_clock_layer);
    layer_add_child(rootLayer, s_worktime_layer);

    // sub-layers
    s_worktime_text_layer = text_layer_create(GRect(0, -4, 100, 30));
    text_layer_set_background_color(s_worktime_text_layer, GColorClear);
    text_layer_set_text_color(s_worktime_text_layer, GColorBlack);
    text_layer_set_font(s_worktime_text_layer, s_font);
    text_layer_set_text_alignment(s_worktime_text_layer, GTextAlignmentCenter);
    // text_layer_set_text(s_worktime_text_layer, "00:00");

    layer_add_child(s_worktime_layer, text_layer_get_layer(s_worktime_text_layer));

    // Create time TextLayer
    text_layer = text_layer_create(GRect(0, 42, 134, 50));
    text_layer_set_background_color(text_layer, GColorClear);
    text_layer_set_text_color(text_layer, GColorBlack);
    text_layer_set_font(text_layer, s_time_font);
    text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);

    // temperature Layer
    s_weather_layer = text_layer_create(GRect(0, 87, 134, 25));
    text_layer_set_background_color(s_weather_layer, GColorClear);
    text_layer_set_text_color(s_weather_layer, GColorBlack);
    text_layer_set_font(s_weather_layer, s_font_22);
    text_layer_set_text_alignment(s_weather_layer, GTextAlignmentCenter);

    s_icon_layer = bitmap_layer_create(GRect(45, 11, 45, 45));

    layer_add_child(s_clock_layer, text_layer_get_layer(text_layer));
    layer_add_child(s_clock_layer, text_layer_get_layer(s_weather_layer));
    layer_add_child(s_clock_layer, bitmap_layer_get_layer(s_icon_layer));
}

static void window_unload(Window *window) {
    // Destroy GBitmap
    gbitmap_destroy(s_background_bitmap);
    gbitmap_destroy(s_icon_bitmap);

    // Destroy BitmapLayer
    bitmap_layer_destroy(s_background_layer);
    bitmap_layer_destroy(s_icon_layer);

    // Unload GFont
    fonts_unload_custom_font(s_font);
    fonts_unload_custom_font(s_time_font);
    fonts_unload_custom_font(s_font_22);

    layer_destroy(s_clock_layer);
    text_layer_destroy(text_layer);
    text_layer_destroy(s_weather_layer);
    text_layer_destroy(s_worktime_text_layer);
    layer_destroy(s_worktime_layer);
}

static void assign_weather_icon(char* conditions_buffer){
    if (s_icon_bitmap) {
        gbitmap_destroy(s_icon_bitmap);
        s_icon_bitmap = NULL;
    }
    if (strcmp(conditions_buffer, "01d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_01d);
    } else if (strcmp(conditions_buffer, "01n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_01n);
    } else if (strcmp(conditions_buffer, "02d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_02d);
    } else if (strcmp(conditions_buffer, "02n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_02n);
    } else if (strcmp(conditions_buffer, "03d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_03d);
    } else if (strcmp(conditions_buffer, "03n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_03d);
    } else if (strcmp(conditions_buffer, "04d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_04d);
    } else if (strcmp(conditions_buffer, "04n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_04d);
    } else if (strcmp(conditions_buffer, "09d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_09d);
    } else if (strcmp(conditions_buffer, "09n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_09d);
    } else if (strcmp(conditions_buffer, "10d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_10d);
    } else if (strcmp(conditions_buffer, "10n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_10d);
    } else if (strcmp(conditions_buffer, "11d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_11d);
    } else if (strcmp(conditions_buffer, "11n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_11d);
    } else if (strcmp(conditions_buffer, "13d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_13d);
    } else if (strcmp(conditions_buffer, "13n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_13d);
    } else if (strcmp(conditions_buffer, "50d") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_50d);
    } else if (strcmp(conditions_buffer, "50n") == 0) {
        s_icon_bitmap = gbitmap_create_with_resource(RESOURCE_ID_ICON_50d);
    }
    if (s_icon_bitmap) {
        bitmap_layer_set_bitmap(s_icon_layer, s_icon_bitmap);
        update_time();
    }
}

static void inbox_received_callback(DictionaryIterator *iterator, void *context) {
    // Read first item
    Tuple *t = dict_read_first(iterator);
    // Store incoming information
    static char temperature_buffer[8];
    static char conditions_buffer[32];
    static char weather_layer_buffer[32];
    static char worktime_buffer[6];

    // For all items
    while(t != NULL) {
        // Which key was received?
        switch(t->key) {
            case KEY_TEMPERATURE:
                snprintf(temperature_buffer, sizeof(temperature_buffer), "%d°C", (int)t->value->int32);
                text_layer_set_text(s_weather_layer, temperature_buffer);
                break;
            case KEY_CONDITIONS:
                snprintf(conditions_buffer, sizeof(conditions_buffer), "%s", t->value->cstring);
                assign_weather_icon(conditions_buffer);
                APP_LOG(APP_LOG_LEVEL_DEBUG, "Loaded weather icon: '%s'", conditions_buffer);
                break;
            case KEY_WORKTIME:
                snprintf(worktime_buffer, sizeof(worktime_buffer), "%s", t->value->cstring);
                text_layer_set_text(s_worktime_text_layer, worktime_buffer);
                break;
            case KEY_BATTERY_PERCENT:
                phone_battery_percent = (int) t->value->int32;
                break;
            default:
                APP_LOG(APP_LOG_LEVEL_ERROR, "Key %d not recognized!", (int)t->key);
                break;
        }

        // Look for next item
        t = dict_read_next(iterator);
    }
}


static void inbox_dropped_callback(AppMessageResult reason, void *context) {
    APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

static void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
    APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

static void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
    APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}

static void send_int_to_phone(int key, int value) {
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    dict_write_int(iter, key, &value, sizeof(int), true);
    app_message_outbox_send();
}

static void battery_handler(BatteryChargeState charge_state) {

    if (charge_state.is_plugged) {
        send_int_to_phone(KEY_WATCH_IS_PLUGGED, VALUE_WATCH_IS_PLUGGED);
    } else {
        send_int_to_phone(KEY_WATCH_IS_PLUGGED, VALUE_WATCH_IS_UNPLUGGED);
    }
}

static void init(void) {
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
            .load = window_load,
            .unload = window_unload,
            });

    const bool animated = true;
    window_stack_push(window, animated);

    // Register with TickTimerService
    tick_timer_service_subscribe(MINUTE_UNIT, tick_handler);
    battery_state_service_subscribe(battery_handler);
    app_message_register_inbox_received(inbox_received_callback);
    app_message_register_inbox_dropped(inbox_dropped_callback);
    app_message_register_outbox_failed(outbox_failed_callback);
    app_message_register_outbox_sent(outbox_sent_callback);

    app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());
    update_time();
}

static void deinit(void) {
  window_destroy(window);
}

int main(void) {
  init();

  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);

  app_event_loop();
  deinit();
}
