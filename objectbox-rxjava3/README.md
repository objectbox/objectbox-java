RxJava 3 APIs for ObjectBox
===========================
While ObjectBox has [data observers and reactive extensions](https://docs.objectbox.io/data-observers-and-rx) built-in,
this project adds RxJava 3 support.  

For general object changes, you can use `RxBoxStore` to create an `Observable`.

`RxQuery` allows you to interact with ObjectBox `Query` objects using:
 * Flowable
 * Observable
 * Single

For example to get query results and subscribe to future updates (Object changes will automatically emmit new data):

```java
Query<User> query = box.query().build();
RxQuery.observable(query).subscribe(this);
```
    
Adding the library to your project
-----------------
Grab via Gradle:
```gradle
implementation "io.objectbox:objectbox-rxjava3:$objectboxVersion"
```

Migrating from RxJava 2
-----------------------

If you have previously used the ObjectBox RxJava library note the following changes:

- The location of the dependency has changed to `objectbox-rxjava3` (see above).
- The package name has changed to `io.objectbox.rx3` (from `io.objectbox.rx`).

This should allow using both versions side-by-side while you migrate your code to RxJava 3.

Links
-----
[Data Observers and Rx Documentation](https://docs.objectbox.io/data-observers-and-rx)
