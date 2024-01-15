package com.niresh23.fanlightcontroller.ble

interface HandlerCode {
    val CMD_RESPONSE: Int
        get() = 100

    val COLOR_RESULT: Int
        get() = 80

    val CONNECT: Int
        get() = 30

    val DESCONNECT: Int
        get() = 40

    val DEVICECONNECTFAIL: Int
        get() = 50

    val DEVICEGET: Int
        get() = 10

    val DEVICENULL: Int
        get() = 20

    val DEVICE_REQUEST: Int
        get() = 200

    val MAPPINGRESULT: Int
        get() = 70

    val MAPPING_RESULT_1: Int
        get() = 91

    val MAPPING_RESULT_2: Int
        get() = 92

    val MAPPING_RESULT_3: Int
        get() = 93

    val MAPPING_RESULT_4: Int
        get() = 94

    val TIMEOUT: Int
        get() = 60
}