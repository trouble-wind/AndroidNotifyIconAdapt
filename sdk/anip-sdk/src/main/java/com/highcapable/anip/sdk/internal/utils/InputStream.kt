/*
 * ANIP - Provides standardized monochrome icon resources for apps and vendor systems that do not conform to the Android standard notification design.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/android-notification-icon-project
 *
 * Apache License Version 2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is created by fankes on 2026/9/4.
 */
package com.highcapable.anip.sdk.internal.utils

import com.highcapable.anip.sdk.internal.exception.BundleException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Reads at most [limit] bytes into memory for the entire stream and fails before reading data beyond that limit.
 * @param limit the maximum number of bytes to read.
 * @return [ByteArray] containing the read bytes.
 */
internal fun InputStream.readLimited(limit: Long) = ByteArrayOutputStream().use { output ->
    copyToLimited(output, limit)
    output.toByteArray()
}

/**
 * Copies at most [limit] bytes and fails before writing data beyond that limit.
 * The unbounded standard `copyTo` cannot reject excess data before it is written.
 * @param output the output stream to write to.
 * @param limit the maximum number of bytes to copy.
 * @return [Long] the number of bytes copied.
 */
internal fun InputStream.copyToLimited(output: OutputStream, limit: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L

    generateSequence { read(buffer).takeIf { it >= 0 } }.forEach { count ->
        if (copied + count > limit) throw BundleException("Stream exceeds the size limit.")

        output.write(buffer, 0, count)
        copied += count
    }
    return copied
}