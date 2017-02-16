package io.objectbox.reactive;

public interface Scheduler {
    <T> void run(RunWithParam runnable, T param);
}
