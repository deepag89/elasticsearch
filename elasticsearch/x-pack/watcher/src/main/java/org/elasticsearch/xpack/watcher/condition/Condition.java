/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 *
 */
public interface Condition extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        public enum Status {
            SUCCESS, FAILURE
        }

        protected final String type;
        protected final Status status;
        private final String reason;
        protected final boolean met;

        public Result(String type, boolean met) {
            // TODO: FAILURE status is never used, but a some code assumes that it is used
            this.status = Status.SUCCESS;
            this.type = type;
            this.met = met;
            this.reason = null;
        }

        public String type() {
            return type;
        }

        public Status status() {
            return status;
        }

        public boolean met() {
            return met;
        }

        public String reason() {
            assert status == Status.FAILURE;
            return reason;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Field.TYPE.getPreferredName(), type);
            builder.field(Field.STATUS.getPreferredName(), status.name().toLowerCase(Locale.ROOT));
            switch (status) {
                case SUCCESS:
                    assert reason == null;
                    builder.field(Field.MET.getPreferredName(), met);
                    break;
                case FAILURE:
                    assert reason != null && !met;
                    builder.field(Field.REASON.getPreferredName(), reason);
                    break;
                default:
                    assert false;
            }
            typeXContent(builder, params);
            return builder.endObject();
        }

        protected abstract XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException;
    }

    interface Builder<C extends Condition> {

        C build();
    }

    interface Field {
        ParseField TYPE = new ParseField("type");
        ParseField STATUS = new ParseField("status");
        ParseField MET = new ParseField("met");
        ParseField REASON = new ParseField("reason");
    }
}
