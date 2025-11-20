package com.mysite.knitly.global.exception

class ServiceException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)