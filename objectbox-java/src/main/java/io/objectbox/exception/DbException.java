/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.exception;

/**
 * General exception for things that may go wrong with the database.
 * Also base class for more concrete exceptions.
 */
public class DbException extends RuntimeException {
    private final int errorCode;

    public DbException(String message) {
        super(message);
        errorCode = 0;
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
        errorCode = 0;
    }

    public DbException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return errorCode == 0 ? super.toString() :
                super.toString() + " (error code " + errorCode + ")";
    }

    /** 0 == no error code available */
    public int getErrorCode() {
        return errorCode;
    }
}
