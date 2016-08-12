/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.condition.ConditionRegistry;
import org.elasticsearch.xpack.watcher.support.validation.Validation;
import org.elasticsearch.xpack.watcher.transform.TransformRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class ActionRegistry {

    private final Map<String, ActionFactory> parsers;
    private final ConditionRegistry conditionRegistry;
    private final TransformRegistry transformRegistry;
    private final Clock clock;
    private final XPackLicenseState licenseState;

    @Inject
    public ActionRegistry(Map<String, ActionFactory> parsers,
                          ConditionRegistry conditionRegistry, TransformRegistry transformRegistry,
                          Clock clock,
                          XPackLicenseState licenseState) {
        this.parsers = parsers;
        this.conditionRegistry = conditionRegistry;
        this.transformRegistry = transformRegistry;
        this.clock = clock;
        this.licenseState = licenseState;
    }

    ActionFactory factory(String type) {
        return parsers.get(type);
    }

    public ExecutableActions parseActions(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse actions for watch [{}]. expected an object but found [{}] instead",
                    watchId, parser.currentToken());
        }
        List<ActionWrapper> actions = new ArrayList<>();

        String id = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                id = parser.currentName();
                Validation.Error error = Validation.actionId(id);
                if (error != null) {
                    throw new ElasticsearchParseException("could not parse action [{}] for watch [{}]. {}", id, watchId, error);
                }
            } else if (token == XContentParser.Token.START_OBJECT && id != null) {
                actions.add(ActionWrapper.parse(watchId, id, parser, this, conditionRegistry, transformRegistry, clock, licenseState));
            }
        }
        return new ExecutableActions(actions);
    }

}
