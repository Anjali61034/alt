package com.navigine.navigine.demo.utils

import com.navigine.navigine.demo.BuildConfig

object Constants {
    const val TAG: String = "NavigineDemo.LOG"

    // deep link query params
    const val DL_QUERY_SERVER: String = "server"
    const val DL_QUERY_USERHASH: String = "userhash"
    const val DL_QUERY_LOCATION_ID: String = "loc"
    const val DL_QUERY_SUBLOCATION_ID: String = "subloc"
    const val DL_QUERY_VENUE_ID: String = "venue_id"

    // notifications
    val NOTIFICATION_CHANNEL_ID: String = BuildConfig.APPLICATION_ID + ".PUSH"
    const val NOTIFICATION_CHANNEL_NAME: String = "NAVIGINE_PUSH"
    const val NOTIFICATION_PUSH_ID: Int = 1
    const val REQUEST_CODE_NOTIFY: Int = 102

    // notifications extras
    const val NOTIFICATION_TITLE: String = "notification_title"
    const val NOTIFICATION_TEXT: String = "notification_text"
    const val NOTIFICATION_IMAGE: String = "notification_image"

    // network
    const val HOST_VERIFY_TAG: String = "verify_request"
    const val ENDPOINT_HEALTH_CHECK: String = "/mobile/health_check"
    const val ENDPOINT_GET_USER: String = "/mobile/v1/users/get?userHash="
    const val RESPONSE_KEY_NAME: String = "name"
    const val RESPONSE_KEY_EMAIl: String = "email"
    const val RESPONSE_KEY_HASH: String = "hash"
    const val RESPONSE_KEY_AVATAR: String = "avatar_url"
    const val RESPONSE_KEY_COMPANY: String = "company_name"

    // anim image sizes
    const val SIZE_SUCCESS: Int = 52
    const val SIZE_FAILED: Int = 32
    const val CHECK_FRAME_SELECTED: Float = 1f

    // broadcast events
    const val LOCATION_CHANGED: String = "LOCATION_CHANGED"
    const val VENUE_SELECTED: String = "VENUE_SELECTED"
    const val VENUE_FILTER_ON: String = "VENUE_FILTER_ON"
    const val VENUE_FILTER_OFF: String = "VENUE_FILTER_OFF"

    // intent keys
    const val KEY_VENUE_SUBLOCATION: String = "venue_sublocation"
    const val KEY_VENUE_POINT: String = "venue_point"
    const val KEY_VENUE_CATEGORY: String = "venue_category"

    // debug mode
    const val LIST_SIZE_DEFAULT: Int = 6

    // circular progress
    const val CIRCULAR_PROGRESS_DELAY_SHOW: Int = 200
    const val CIRCULAR_PROGRESS_DELAY_HIDE: Int = 700
}
