package org.protempa.backend.dsb.file;

/*
 * #%L
 * Protempa File Data Source Backend
 * %%
 * Copyright (C) 2012 - 2015 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.io.File;
import org.protempa.DataSourceReadException;
import org.protempa.DataStreamingEvent;
import org.protempa.proposition.Proposition;

/**
 *
 * @author Andrew Post
 */
public class FixedWidthFileLineIterator extends AbstractFileLineIterator {
    private final FixedWidthColumnSpec[] columnSpecs;
    private int keyIdLength;
    private int keyIdOffset;
    private int[] rowSpecs;

    public FixedWidthFileLineIterator(File file, int skipLines, int keyIdOffset, int keyIdLength, FixedWidthColumnSpec[] columnSpecs, int[] rowSpecs, String id) throws DataSourceReadException {
        super(file, skipLines, id);
        if (columnSpecs == null) {
            throw new IllegalArgumentException("columnSpecs cannot be null");
        }
        if (rowSpecs != null && rowSpecs.length != columnSpecs.length) {
            throw new IllegalArgumentException("if rowSpecs is not null, it must have the same length as columnSpecs");
        }
        this.columnSpecs = columnSpecs.clone();
        if (rowSpecs != null) {
            this.rowSpecs = rowSpecs.clone();
        }
        this.keyIdOffset = keyIdOffset;
        this.keyIdLength = keyIdLength;

        for (FixedWidthColumnSpec spec : columnSpecs) {
            /*
             * Some files don't pad the last column with trailing whitespace.
             * To account for this, we just require the last column to have at
             * least 1 character of data.
             */
            setRequiredRowLength(Math.max(spec.getOffset() + 1, getRequiredRowLength()));
        }
    }

    @Override
    protected DataStreamingEvent<Proposition> dataStreamingEvent() throws DataSourceReadException {
        char[] charArray = getCurrentLine().toCharArray();
        String keyId = String.copyValueOf(charArray, this.keyIdOffset, this.keyIdLength);
        int colNum = 0;
        for (int i = 0; i < this.columnSpecs.length; i++) {
            if (this.rowSpecs == null || this.rowSpecs[i] == getLineNumber()) {
                FixedWidthColumnSpec colSpec = this.columnSpecs[i];
                /**
                 * Some files don't pad the last column with trailing
                 * whitespace. Hence, we need to check the length of the line.
                 */
                String column = String.copyValueOf(charArray, colSpec.getOffset(), Math.min(charArray.length - colSpec.getOffset(), colSpec.getLength())).trim();
                String links = colSpec.getLinks();
                parseLinks(links, column, colNum++);
            }
        }
        if (keyId == null) {
            throw new DataSourceReadException("keyId was never set");
        }
        return new DataStreamingEvent(keyId, getData());
    }

}
