/*
 * Copyright 2024 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.query;

/**
 * Wraps the ID of a matching object and a score when using {@link Query#findIdsWithScores}.
 */
public class IdWithScore {

    private final long id;
    private final double score;

    // Note: this constructor is called by JNI, check before modifying/removing it.
    public IdWithScore(long id, double score) {
        this.id = id;
        this.score = score;
    }

    /**
     * Returns the object ID.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the query score for the {@link #getId() id}.
     * <p>
     * The query score indicates some quality measurement. E.g. for vector nearest neighbor searches, the score is the
     * distance to the given vector.
     */
    public double getScore() {
        return score;
    }
}
