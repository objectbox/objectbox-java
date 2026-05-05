# ObjectBox Android APIs

Builds Android libraries (AAR) published as Maven artifacts to [use ObjectBox on Android](../README.md#getting-started).

Each Android library contains Android-specific ObjectBox APIs and has a dependency on a matching Android database library.

Available public APIs:

- [Admin](src/adminIncluded/java/io/objectbox/android/Admin.java): API to use the [Admin web app on Android](https://docs.objectbox.io/data-browser#admin-for-android)
- [AndroidScheduler](src/main/java/io/objectbox/android/AndroidScheduler.java): can be used to [observe query results](https://docs.objectbox.io/data-observers-and-rx#thread-scheduling) on the Android main thread
- [ObjectBoxDataSource](src/main/java/io/objectbox/android/ObjectBoxDataSource.java): reference implementation of a data source for the AndroidX paging library
- [ObjectBoxLiveData](src/main/java/io/objectbox/android/ObjectBoxLiveData.java): reference implementation of an AndroidX LiveData

The following Maven artifacts are currently published by this project:

| Artifact ID                            | Admin                                              | Android database library |
|----------------------------------------|----------------------------------------------------|--------------------------|
| `objectbox-android`                    | no-op API                                          | Basic                    |
| `objectbox-android-objectbrowser`      | API, [resources][res], [custom manifest][manifest] | Basic + Admin            |
| `objectbox-sync-android`               | no-op API                                          | Sync                     |
| `objectbox-sync-android-objectbrowser` | API, [resources][res], [custom manifest][manifest] | Sync + Admin             |

[manifest]: src/adminIncluded/AndroidManifest.xml
[res]: src/adminIncluded/res
