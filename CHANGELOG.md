# Changelog

Notable changes to the ObjectBox Java library.

For more insights into what changed in the database libraries, [check the ObjectBox C changelog](https://github.com/objectbox/objectbox-c/blob/main/CHANGELOG.md).

## 5.2.0 - 2026-02-16

- The [ObjectBox Gradle plugin](https://github.com/objectbox/objectbox-java-generator) requires JDK 11 and Android
  Gradle Plugin 8.1 or newer.
- Update database libraries for Android and JVM to database version `5.1.1-pre-2026-02-16`.
- Admin: new "Counts and Sizes" view (via Status page) to give an overview over all types their object counts and sizes
- Admin: fix reload via browser to stay on the page
- Admin: schema view to display flags as text
- Admin: refresh button for the data view

### Sync

- Add simplified `Sync.client(boxStore)` helper method. Move URL and credentials options to builder, add variants that 
  accept multiple URLs and credentials. Deprecate the existing helper methods.
- Add Sync client builder option to configure Sync behavior using 
  [SyncFlags](objectbox-java/src/main/java/io/objectbox/sync/SyncFlags.java).

## 5.1.0 - 2026-01-26

- Add [ObjectBoxThreadPoolExecutor](objectbox-java/src/main/java/io/objectbox/ObjectBoxThreadPoolExecutor.java), a
  default implementation of a `ThreadPoolExecutor` that properly cleans up thread-local Box resources.
- Add methods `newCachedThreadPoolExecutor()` and `newFixedThreadPoolExecutor()` to `BoxStore` to help create instances
  of `ObjectBoxThreadPoolExecutor` for common uses.
- Add methods `newCachedThreadPoolDispatcher()` and `newFixedThreadPoolDispatcher()` to the Kotlin extension functions
  for `BoxStore` to help create common coroutine dispatchers backed by an `ObjectBoxThreadPoolExecutor`.
- `BoxStore.runInTx` and `callInTx` close a write cursor even if the runnable or callable throws. This would previously
  result in cursor not closed warnings when the cursor was closed by the finalizer daemon. 
- `BoxStore` no longer gets stuck while closing when deleting transactions created in other threads with open relation
  cursors.
- Admin: the Schema view now shows if external name of types and properties if configured.
- Admin: the Schema view now shows the type as text (e.g. "String") instead of the internal type ID.
                          
### Sync

- New Sync protocol V8: using new clients also requires a server update
- Remove superfluous sync listener triggers when sync filters "report updates" (SKIP_TX)
- Sync clients compress earlier: reduces disk storage for outgoing data
- Reworked certificates for Apple platforms
- Removed support for older Sync protocol versions before 2024-09; protocol V5+ is now required.

## 5.0.1 - 2025-09-30

- Update runtime libraries for Android and JVM to database version `5.0.0-2025-09-27`.
- Fix a race condition with a closing store and still active transactions that kept the store from closing.
  For Android this may fix some rare ANR issues.

### Sync

- Add `filterVariables` option to `SyncClient` builder for [Sync filters](https://sync.objectbox.io/sync-server/sync-filters)
  introduced in version 5.0.

## 5.0.0 - 2025-09-17

- Includes runtime libraries for Android and JVM with database version `5.0.0-2025-09-16`.
  - Android: Prior to Android 8.0, don't crash when inserting objects with string lists whose size exceeds the local
    reference table size. [#1215](https://github.com/objectbox/objectbox-java/issues/1215) 
  - ToOne relations: when deleting an object with an ID larger than the maximum 32-bit unsigned integer 
    (`4_294_967_295`) that is used as the target object of a ToOne, correctly re-set the target ID of the ToOne to
    `0`. [objectbox-dart#740](https://github.com/objectbox/objectbox-dart/issues/740) 
- When re-creating a `BoxStore` for the same directory and `close()` wasn't called on the previous instance, don't throw
  an "Another BoxStore is still open for this directory" exception. Note that calling `close()` *is recommended* before 
  creating a new instance. [#1201](https://github.com/objectbox/objectbox-java/issues/1201)
- When using `BoxStoreBuilder.buildDefault()`, don't leak the Store when setting it as default fails.
- To help diagnose, print stacks of all threads in the internal thread pool if shutting it down takes too long when
  closing `BoxStore`.
- Remove deprecated APIs:
  - `Query.setParameters` methods that set a single parameter, use the `setParameter` methods instead.
  - `Box.removeByKeys`, use `Box.removeByIds` instead.
  - `BoxStore.sizeOnDisk`, use `getDbSize` or `getDbSizeOnDisk` instead which properly handle in-memory databases.
  - `BoxStoreBuilder.debugTransactions`, use `debugFlags(DebugFlags.LOG_TRANSACTIONS_READ | DebugFlags.LOG_TRANSACTIONS_WRITE)` instead.
  - `SyncServerBuilder` `peer` configuration options, use the `clusterPeer` options instead.
  - `io.objectbox.DebugFlags`, use `io.objectbox.config.DebugFlags` instead.
  - `ValidateOnOpenMode` constants, use `ValidateOnOpenModePages` instead.
  - DAOcompat compatibility query methods. Use the regular query API instead.           

### Sync

- Support Sync server version 5.0.
  - **User-Specific Data Sync**: support configuring [Sync filter](https://sync.objectbox.io/sync-server/sync-filters) 
    variables on `SyncClient`.

## 4.3.1 - 2025-08-12

- Requires at least Kotlin compiler and standard library 1.7.
- Data Observers: closing a Query now waits on a running publisher to finish its query, preventing a VM crash. [#1147](https://github.com/objectbox/objectbox-java/issues/1147)
- Update database libraries for Android and JVM to version `4.3.1` (include database version `4.3.1-2025-08-02`).

## 4.3.0 - 2025-05-13

- Basic support for boolean array properties (`boolean[]` in Java or `BooleanArray` in Kotlin).
- The Windows database library now statically links the MSVC runtime to avoid crashes in incompatible `msvcp140.dll` 
  shipped with some JDKs.
- External property types (via [MongoDB connector](https://sync.objectbox.io/mongodb-sync-connector)):
  - add `JSON_TO_NATIVE` to support sub (embedded/nested) documents/arrays in MongoDB
  - support ID mapping to UUIDs (v4 and v7)
- Admin: add class and dependency diagrams to the schema page (view and download).
- Admin: improved data view for large vectors by displaying only the first elements and the full vector in a dialog.
- Admin: detects images stored as bytes and shows them as such (PNG, GIF, JPEG, SVG, WEBP).

### Sync

- Add "Log Events" for important server events, which can be viewed on a new Admin page.
- Detect and ignore changes for objects that were put but were unchanged.
- The limit for message size was raised to 32 MB.
- Transactions above the message size limit now already fail on the client (to better enforce the limit).

## 4.2.0 - 2025-03-04

- Add new query conditions `equalKeyValue`, `greaterKeyValue`, `lessKeyValue`, `lessOrEqualKeyValue`, and 
  `greaterOrEqualKeyValue` that are helpful to write complex queries for [string maps](https://docs.objectbox.io/advanced/custom-types#flex-properties).
  These methods support `String`, `long` and `double` data types for the values in the string map.
- Deprecate the `containsKeyValue` condition, use the new `equalKeyValue` condition instead.
- Android: to build, at least Android Plugin 8.1.1 and Gradle 8.2.1 are required.

## 4.1.0 - 2025-01-30

- Vector Search: add new `VectorDistanceType.GEO` distance type to perform vector searches on geographical coordinates.
  This is particularly useful for location-based applications.
- Android: require Android 5.0 (API level 21) or higher.
- Note on Windows JVM: We've seen crashes on Windows when creating a BoxStore on some JVM versions.
  If this should happen to you, make sure to update your JVM to the latest patch release
  (8.0.432+6, 11.0.25+9, 17.0.13+11 and 21.0.5+11-LTS are known to work).

### Sync

- Add JWT authentication
- Sync clients can now send multiple credentials for login

## 4.0.3 - 2024-10-15

- Make closing the Store more robust. In addition to transactions, it also waits for ongoing queries. This is just an
  additional safety net. Your apps should still make sure to finish all Store operations, like queries, before closing it.
- [Flex properties](https://docs.objectbox.io/advanced/custom-types#flex-properties) support `null` map and list values.
- Some minor vector search performance improvements.

### Sync

- **Fix a serious regression, please update as soon as possible.**
- Add new options, notably for cluster configuration, when building `SyncServer`. Improve documentation.
  Deprecate the old peer options in favor of the new cluster options.
- Add `SyncHybrid`, a combination of a Sync client and a Sync server. It can be used in local cluster setups, in
  which a "hybrid" functions as a client & cluster peer (server).

## 4.0.2 - 2024-08-20

- Add convenience `oneOf` and `notOneOf` conditions that accept `Date` to avoid manual conversion using `getTime()`.
- When `BoxStore` is closing, briefly wait on active transactions to finish.
- Guard against crashes when `BoxStore` was closed, but database operations do still occur concurrently (transactions are still active).

## 4.0.1 - 2024-06-03

- Examples: added [Vector Search example](https://github.com/objectbox/objectbox-examples/tree/main/java-main-vector-search) that demonstrates how to perform on-device [approximate nearest neighbor (ANN) search](https://docs.objectbox.io/on-device-vector-search).
- Revert deprecation of `Box.query()`, it is still useful for queries without any condition.
- Add note on old query API methods of `QueryBuilder` that they are not recommended for new projects. Use [the new query APIs](https://docs.objectbox.io/queries) instead.
- Update and expand documentation on `ToOne` and `ToMany`.

## 4.0.0 - Vector Search - 2024-05-16

**ObjectBox now supports** [**Vector Search**](https://docs.objectbox.io/ann-vector-search) to enable efficient similarity searches.

This is particularly useful for AI/ML/RAG applications, e.g. image, audio, or text similarity. Other use cases include semantic search or recommendation engines.

Create a Vector (HNSW) index for a floating point vector property. For example, a `City` with a location vector:

```java
@Entity
public class City {

    @HnswIndex(dimensions = 2)
    float[] location;
    
}
```

Perform a nearest neighbor search using the new `nearestNeighbors(queryVector, maxResultCount)` query condition and the new "find with scores" query methods (the score is the distance to the query vector). For example, find the 2 closest cities:

```java
final float[] madrid = {40.416775F, -3.703790F};
final Query<City> query = box
        .query(City_.location.nearestNeighbors(madrid, 2))
        .build();
final City closest = query.findWithScores().get(0).get();
```

For an introduction to Vector Search, more details and other supported languages see the [Vector Search documentation](https://docs.objectbox.io/ann-vector-search).

- BoxStore: deprecated `BoxStore.sizeOnDisk()`. Instead use one of the new APIs to determine the size of a database:
  - `BoxStore.getDbSize()` which for a file-based database returns the file size and for an in-memory database returns the approximately used memory,
  - `BoxStore.getDbSizeOnDisk()` which only returns a non-zero size for a file-based database.
- Query: add properly named `setParameter(prop, value)` methods that only accept a single parameter value, deprecated the old `setParameters(prop, value)` variants.
- Sync: add `SyncCredentials.userAndPassword(user, password)`.
- Gradle plugin: the license of the [Gradle plugin](https://github.com/objectbox/objectbox-java-generator) has changed to the GNU Affero General Public License (AGPL).

## Previous versions

See the [Changelogs in the documentation](https://docs.objectbox.io/changelogs).
