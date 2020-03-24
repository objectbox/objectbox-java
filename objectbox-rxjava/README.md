:information_source: This library will receive no new features.
Development will continue with the [RxJava 3 APIs for ObjectBox](/objectbox-rxjava3).

RxJava 2 APIs for ObjectBox
===========================
While ObjectBox has [data observers and reactive extensions](https://docs.objectbox.io/data-observers-and-rx) built-in,
this project adds RxJava 2 support.  

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
implementation "io.objectbox:objectbox-rxjava:$objectboxVersion"
```

Links
-----
[Data Observers and Rx Documentation](https://docs.objectbox.io/data-observers-and-rx)
