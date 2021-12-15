<img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png">

# ObjectBox Java (Kotlin, Android)
[ObjectBox](https://objectbox.io/) is a superfast object-oriented database with strong relation support.
ObjectBox is embedded into your Android, Linux, macOS, or Windows app.

**Latest version: [3.1.0 (2021/12/15)](https://docs.objectbox.io/#objectbox-changelog)**

Demo code using ObjectBox:

```kotlin
// Kotlin
val playlist = Playlist("My Favorites")
playlist.songs.add(Song("Lalala"))
playlist.songs.add(Song("Lololo"))
box.put(playlist)
```

```java
// Java
Playlist playlist = new Playlist("My Favorites");
playlist.songs.add(new Song("Lalala"));
playlist.songs.add(new Song("Lololo"));
box.put(playlist);
```

Other languages/bindings
------------------------
ObjectBox supports multiple platforms and languages.
Besides JVM based languages like Java and Kotlin, ObjectBox also offers: 

* [ObjectBox Swift](https://github.com/objectbox/objectbox-swift): build fast mobile apps for iOS (and macOS) 
* [ObjectBox Dart/Flutter](https://github.com/objectbox/objectbox-dart): cross-platform for mobile and desktop apps 
* [ObjectBox Go](https://github.com/objectbox/objectbox-go): great for data-driven tools and embedded server applications 
* [ObjectBox C and C++](https://github.com/objectbox/objectbox-c): native speed with zero copy access to FlatBuffer objects

Gradle setup
------------
For Android projects, add the ObjectBox Gradle plugin to your root `build.gradle`: 

```groovy
buildscript {
    ext.objectboxVersion = "3.1.0"
    repositories {        
        mavenCentral()    
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}
```

And in your app's `build.gradle` apply the plugin:

```groovy
// Using plugins syntax:
plugins {
    id("io.objectbox") // Add after other plugins.
}

// Or using the old apply syntax:
apply plugin: "io.objectbox" // Add after other plugins.
```

First steps
-----------
Create a data object class `@Entity`, for example "Playlist".
```
// Kotlin
@Entity data class Playlist( ... )

// Java
@Entity public class Playlist { ... }
```
Now build the project to let ObjectBox generate the class `MyObjectBox` for you.

Prepare the BoxStore object once for your app, e.g. in `onCreate` in your Application class:

```java
boxStore = MyObjectBox.builder().androidContext(this).build();
```

Then get a `Box` class for the Playlist entity class:

```java
Box<Playlist> box = boxStore.boxFor(Playlist.class);
```

The `Box` object gives you access to all major functions, like `put`, `get`, `remove`, and `query`.

For details please check the [docs](https://docs.objectbox.io).     

Links
-----
[Features](https://objectbox.io/features/)

[Docs & Changelog](https://docs.objectbox.io/), [JavaDocs](https://objectbox.io/docfiles/java/current/)

[Examples](https://github.com/objectbox/objectbox-examples)

[![Follow ObjectBox on Twitter](https://img.shields.io/twitter/follow/ObjectBox_io.svg?style=flat-square&logo=twitter)](https://twitter.com/intent/follow?screen_name=ObjectBox_io)

License
-------
    Copyright 2017-2021 ObjectBox Ltd. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

