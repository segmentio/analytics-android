package com.segment.jsmiddleware

import java.io.BufferedReader
import java.io.InputStream

fun readInputStream(inputStream: InputStream): String {
    return inputStream.bufferedReader().use(BufferedReader::readText)
}