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
import au.com.bytecode.opencsv.CSVParser;
import java.io.File;
import java.io.IOException;
import org.protempa.DataSourceReadException;
import org.protempa.DataStreamingEvent;
import org.protempa.proposition.Proposition;

/**
 *
 * @author Andrew Post
 */
public class DelimitedFileLineIterator extends AbstractFileLineIterator {

    private final int[] rowSpecs;
    private final DelimitedColumnSpec[] columnSpecs;
    private final String keyId;
    private final DelimitedColumnSpec[] keyIdColumnSpecs;
    private int keyIdIndex;
    private final CSVParser csvParser;

    public DelimitedFileLineIterator(File file, int skipLines, String keyId,
            int keyIdIndex, char delimiter,
            DelimitedColumnSpec[] keyIdColumnSpecs,
            DelimitedColumnSpec[] columnSpecs, int[] rowSpecs, String id)
            throws DataSourceReadException {
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
        } else {
            this.rowSpecs = null;
        }
        this.keyIdIndex = keyIdIndex;
        this.csvParser = new CSVParser(delimiter);
        this.keyId = keyId;
        if (keyIdColumnSpecs != null) {
            this.keyIdColumnSpecs = keyIdColumnSpecs.clone();
        } else {
            this.keyIdColumnSpecs = null;
        }
    }

    @Override
    protected DataStreamingEvent<Proposition> dataStreamingEvent()
            throws DataSourceReadException {
        try {
            String[] line = this.csvParser.parseLine(getCurrentLine());
            String kId = this.keyIdIndex > -1 ? line[this.keyIdIndex] : this.keyId;
            if (kId == null) {
                throw new DataSourceReadException("keyId was never set");
            }
            if (this.keyIdColumnSpecs != null) {
                for (DelimitedColumnSpec colSpec : this.keyIdColumnSpecs) {
                    parseLinks(colSpec.getLinks(), this.keyId, -1);
                }
            }
            int colNum = 0;
            for (int i = 0; i < this.columnSpecs.length; i++) {
                if (this.rowSpecs == null || this.rowSpecs[i] == getLineNumber()) {
                    DelimitedColumnSpec colSpec = this.columnSpecs[i];
                    String column = line[colSpec.getIndex()].trim();
                    String links = colSpec.getLinks();
                    parseLinks(links, column, colNum++);
                }
            }
            return new DataStreamingEvent(kId, getData());
        } catch (IOException ex) {
            throw new DataSourceReadException(ex);
        }
    }

}
