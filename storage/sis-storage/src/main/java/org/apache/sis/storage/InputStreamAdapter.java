/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage;

import java.io.InputStream;
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.internal.storage.Trackable;


/**
 * Wraps a {@link ImageInputStream} as a standard {@link InputStream}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4
 * @version 0.8
 * @module
 */
final class InputStreamAdapter extends InputStream implements Trackable {
    /**
     * The data input stream.
     */
    final ImageInputStream input;

    /**
     * Constructs a new input stream.
     */
    InputStreamAdapter(final ImageInputStream input) {
        this.input = input;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        return input.read();
    }

    /**
     * Reads some number of bytes from the input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return input.read(b);
    }

    /**
     * Reads up to {@code len} bytes of data from the input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return input.read(b, off, len);
    }

    /**
     * Skips over and discards {@code n} bytes of data from this input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public long skip(final long n) throws IOException {
        return input.skipBytes(n);
    }

    /**
     * Returns always {@code true}, since marks support is mandatory in image input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Marks the current position in this input stream.
     *
     * @param  readlimit ignored.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void mark(final int readlimit) {
        input.mark();
    }

    /**
     * Marks the current position in this input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void mark() {
        input.mark();
    }

    /**
     * Repositions this stream to the position at the time the {@code mark} method was last called.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void reset() throws IOException {
        input.reset();
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     * @throws IOException if the position can not be obtained.
     */
    @Override
    public long getStreamPosition() throws IOException {
        return input.getStreamPosition();
    }

    /**
     * Closes this input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        input.close();
    }
}
