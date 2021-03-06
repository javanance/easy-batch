/*
 * The MIT License
 *
 *   Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package org.jeasy.batch.core.reader;

import org.jeasy.batch.core.util.Utils;
import org.jeasy.batch.core.record.Record;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Template class for multi-files record readers.
 * Implementations should provide how to create the delegate reader
 * in {@link AbstractMultiFileRecordReader#createReader()} method.
 *
 * Using multi-files readers assumes <strong>all files have the same format</strong>.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 * @param <P> type of the record's payload
 */
public abstract class AbstractMultiFileRecordReader<P> implements RecordReader<P> {

    protected List<Path> files;
    protected Path currentFile;
    protected AbstractFileRecordReader<P> delegate;
    protected Iterator<Path> iterator;
    protected Charset charset;

    /**
     * Create a new multi-file record reader.
     *
     * @param files to read
     */
    public AbstractMultiFileRecordReader(List<Path> files) {
        this(files, Charset.defaultCharset());
    }

    /**
     * Create a new multi-file record reader.
     *
     * @param files to read
     * @param charset of the file
     */
    public AbstractMultiFileRecordReader(List<Path> files, Charset charset) {
        Utils.checkNotNull(files, "files");
        Utils.checkNotNull(charset, "charset");
        this.files = files;
        this.charset = charset;
    }

    @Override
    public void open() throws Exception {
        iterator = files.iterator();
        currentFile = iterator.next();
        if (currentFile != null) {
            delegate = createReader();
            delegate.open();
        }
    }

    @Override
    public Record<P> readRecord() throws Exception {
        if (delegate == null) {
            return null;
        }
        Record<P> record = delegate.readRecord();
        if (record == null) { // finished reading the current file, jump to next file
            delegate.close();
            if (iterator.hasNext()) {
                currentFile = iterator.next();
                delegate = createReader();
                delegate.open();
                return readRecord();
            }
        }
        return record;
    }

    @Override
    public void close() throws Exception {
        if (delegate != null) {
            delegate.close();
        }
    }

    protected abstract AbstractFileRecordReader<P> createReader() throws Exception;
}
