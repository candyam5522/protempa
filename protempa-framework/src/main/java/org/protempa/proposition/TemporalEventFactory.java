/*
 * #%L
 * Protempa Framework
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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
package org.protempa.proposition;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.protempa.SourceSystem;
import org.protempa.proposition.interval.IntervalFactory;
import org.protempa.proposition.value.AbsoluteTimeGranularityUtil;
import org.protempa.proposition.value.Granularity;

/**
 * Generate instances of {@link Event} based on the provided date format and
 * granularity.
 * <b>NOTE:</b> This class is not thread-safe, as it uses a {@link DateFormat}
 * instance field to parse dates from strings.
 */
public final class TemporalEventFactory {

    private static final IntervalFactory intervalFactory
            = new IntervalFactory();
    private DateFormat dateFormat;
    private Granularity granularity;
    private final UniqueIdFactory factory;

    public TemporalEventFactory(DateFormat dateFormat,
            Granularity granularity, UniqueIdFactory factory) {
        if (dateFormat == null) {
            this.dateFormat = DateFormat.getDateTimeInstance();
        } else {
            this.dateFormat = dateFormat;
        }
        this.granularity = granularity;
        if (factory != null) {
            this.factory = factory;
        } else {
            this.factory = new DefaultUniqueIdFactory();
        }
    }

    public Event getInstance(String id, String timestamp,
            SourceSystem dataSourceType) throws ParseException {
        return getInstance(id, timestamp != null
                ? this.dateFormat.parse(timestamp) : null, dataSourceType);
    }

    public Event getInstance(String id, Date timestamp,
            SourceSystem dataSourceType) {
        Long tstampAsPos = AbsoluteTimeGranularityUtil.asPosition(timestamp);
        return getInstance(id, tstampAsPos, dataSourceType);
    }

    public Event getInstance(String id, String start, String finish,
            SourceSystem dataSourceType)
            throws ParseException {

        return getInstance(id, start != null ? this.dateFormat.parse(start)
                : null, finish != null ? this.dateFormat.parse(finish) : null,
                dataSourceType);
    }

    public Event getInstance(String id, Date start, Date finish,
            SourceSystem dataSourceType) {
        Long startAsPos = AbsoluteTimeGranularityUtil.asPosition(start);
        Long finishAsPos = AbsoluteTimeGranularityUtil.asPosition(finish);
        return getInstance(id, startAsPos, finishAsPos,
                dataSourceType);
    }

    private Event getInstance(String id, Long pos,
            SourceSystem dataSourceType) {
        Event pp = new Event(id, this.factory.getInstance());
        pp.setSourceSystem(dataSourceType);
        pp.setInterval(intervalFactory.getInstance(pos, this.granularity,
                pos, this.granularity));
        return pp;
    }

    private Event getInstance(String id, Long start, Long finish,
            SourceSystem dataSourceType) {
        Event e = new Event(id, this.factory.getInstance());
        e.setSourceSystem(dataSourceType);
        e.setInterval(intervalFactory.getInstance(start,
                this.granularity, finish, this.granularity));
        return e;
    }
}
