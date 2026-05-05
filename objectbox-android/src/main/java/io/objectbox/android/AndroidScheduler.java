/*
 * Copyright 2017 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.android;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.Deque;

import androidx.annotation.NonNull;
import io.objectbox.reactive.RunWithParam;
import io.objectbox.reactive.Scheduler;

/**
 * A Looper-based Scheduler implementation, see {@link #mainThread()} for most common usage.
 */
public class AndroidScheduler extends Handler implements Scheduler {
    private static AndroidScheduler MAIN_THREAD;

    /**
     * Returns a Scheduler that runs tasks on Android's main thread.
     */
    public static synchronized Scheduler mainThread() {
        if (MAIN_THREAD == null) {
            MAIN_THREAD = new AndroidScheduler(Looper.getMainLooper());
        }
        return MAIN_THREAD;
    }

    private final Deque<Runner> freeRunners = new ArrayDeque<>();

    /**
     * If you run your own Looper, you can create a custom Scheduler using it.
     */
    public AndroidScheduler(Looper looper) {
        super(looper);
    }

    // Note: need to cast to RunWithParam<Object>, not sure how to solve differently.
    @SuppressWarnings("unchecked")
    @Override
    public <T> void run(@NonNull RunWithParam<T> runnable, @NonNull T param) {
        Runner runner;
        synchronized (freeRunners) {
            runner = freeRunners.poll();
        }
        if (runner == null) {
            runner = new Runner();
        }
        runner.runWithParam = (RunWithParam<Object>) runnable;
        runner.param = param;
        post(runner);
    }

    class Runner implements Runnable {
        RunWithParam<Object> runWithParam;
        Object param;

        @Override
        public void run() {
            runWithParam.run(param);
            runWithParam = null;
            param = null;
            synchronized (freeRunners) {
                if (freeRunners.size() < 20) {
                    freeRunners.add(this);
                }
            }
        }
    }

}
