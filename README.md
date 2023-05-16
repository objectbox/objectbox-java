<p align="center"><img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png"></p>

<p align="center">
  <a href="https://docs.objectbox.io/getting-started">Getting Started</a> •
  <a href="https://docs.objectbox.io">Documentation</a> •
  <a href="https://github.com/objectbox/objectbox-examples">Example Apps</a> •
  <a href="https://github.com/objectbox/objectbox-java/issues">Issues</a>
</p>

<p align="center">
  <a href="https://docs.objectbox.io/#objectbox-changelog">
    <img src="https://img.shields.io/github/v/release/objectbox/objectbox-java?color=7DDC7D&style=flat-square" alt="Latest Release">
  </a>
  <a href="https://github.com/objectbox/objectbox-java/stargazers">
    <img src="https://img.shields.io/github/stars/objectbox/objectbox-java?color=17A6A6&logo=github&style=flat-square" alt="Star objectbox-java">
  </a>
  <a href="https://github.com/objectbox/objectbox-java/blob/main/LICENSE.txt">
    <img src="https://img.shields.io/github/license/objectbox/objectbox-java?color=7DDC7D&logo=apache&style=flat-square" alt="Apache 2.0 license">
  </a>
  <a href="https://twitter.com/ObjectBox_io">
    <img src="https://img.shields.io/twitter/follow/objectbox_io?color=%20%2300aced&logo=twitter&style=flat-square" alt="Follow @ObjectBox_io">
  </a>
</p>

# ObjectBox Java Database (Kotlin, Android)

Java database - simple but powerful, frugal but fast. Embedded into your Android, Linux, macOS, iOS, or Windows app, store and manage data easily, enjoy ludicrous speed, build ecoconciously 💚 

### Demo code

```java
// Java
Playlist playlist = new Playlist("My Favorites");
playlist.songs.add(new Song("Lalala"));
playlist.songs.add(new Song("Lololo"));
box.put(playlist);
```
--> [More details in the docs](https://docs.objectbox.io/)

```kotlin
// Kotlin
val playlist = Playlist("My Favorites")
playlist.songs.add(Song("Lalala"))
playlist.songs.add(Song("Lololo"))
box.put(playlist)
```

## Table of Contents
- [Why use ObjectBox](#why-use-objectbox-for-java-data-management--kotlin-data-management)
  - [Features](#features)
- [How to get started](#how-to-get-started)
  - [Gradle setup](#gradle-setup)
  - [First steps](#first-steps)
- [Already using ObjectBox?](#already-using-objectbox)
- [Other languages/bindings](#other-languagesbindings)
- [License](#license)


## Why use ObjectBox for Java data management / Kotlin data management?

The NoSQL Java database is built for storing data locally, offline-first on resource-restricted devices like phones.

The database is optimized for high speed and low resource consumption on restricted devices, making it ideal for use on mobile devices. It uses minimal CPU, RAM, and power, which is not only great for users but also for the environment.

Being fully ACID-compliant, ObjectBox is faster than any alternative, outperforming SQLite and Realm across all CRUD (Create, Read, Update, Delete) operations. Check out our [Performance Benchmarking App repository](https://github.com/objectbox/objectbox-performance).

Our concise native-language API is easy to pick up and only requires a fraction of the code compared to SQLite. No more rows or columns, just plain objects (true POJOS) with built-in relations. It's great for handling large data volumes and allows changing your model whenever needed.

All of this makes ObjectBox a smart choice for local data persistence with Java and Kotlin - it's efficient, easy and sustainable.

### Features

🏁 **High performance** on restricted devices, like IoT gateways, micro controllers, ECUs etc.\
💚 **Resourceful** with minimal CPU, power and Memory usage for maximum flexibility and sustainability\
🔗 **[Relations](https://docs.objectbox.io/relations):** object links / relationships are built-in\
💻 **Multiplatform:** Linux, Windows, Android, iOS, macOS, any POSIX system

🌱 **Scalable:** handling millions of objects resource-efficiently with ease\
💐 **[Queries](https://docs.objectbox.io/queries):** filter data as needed, even across relations\
🦮 **Statically typed:** compile time checks & optimizations\
📃 **Automatic schema migrations:** no update scripts needed

**And much more than just data persistence**\
🔄 **[ObjectBox Sync](https://objectbox.io/sync/):** keeps data in sync between devices and servers\
🕒 **[ObjectBox TS](https://objectbox.io/time-series-database/):** time series extension for time based data

## How to get started
### Gradle setup

For Android projects, add the ObjectBox Gradle plugin to your root `build.gradle`: 

```groovy
buildscript {
    ext.objectboxVersion = "3.6.0"
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

### First steps

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

## Already using ObjectBox?

❤ **Your opinion matters to us!** Please fill in this 2-minute [Anonymous Feedback Form](https://forms.gle/bdktGBUmL4m48ruj7).

We believe, ObjectBox is super easy to use. We want to bring joy and delight to app developers with intuitive and fun to code with APIs. To do that, we want your feedback: what do you love? What's amiss? Where do you struggle in everyday app development?

**We're looking forward to receiving your comments and requests:**
- Add [GitHub issues](https://github.com/ObjectBox/objectbox-java/issues) 
- Upvote issues you find important by hitting the 👍/+1 reaction button
- Drop us a line via [@ObjectBox_io](https://twitter.com/ObjectBox_io/) or contact[at]objectbox.io
- ⭐ us, if you like what you see 

Thank you! 🙏

Keep in touch: For general news on ObjectBox, [check our blog](https://objectbox.io/blog)!

## Other languages/bindings

ObjectBox supports multiple platforms and languages.
Besides JVM based languages like Java and Kotlin, ObjectBox also offers: 

* [Swift Database](https://github.com/objectbox/objectbox-swift): build fast mobile apps for iOS (and macOS) 
* [Dart/Flutter Database](https://github.com/objectbox/objectbox-dart): cross-platform for mobile and desktop apps 
* [Go Database](https://github.com/objectbox/objectbox-go): great for data-driven tools and embedded server applications 
* [C and C++ Database](https://github.com/objectbox/objectbox-c): native speed with zero copy access to FlatBuffer objects


## License

    Copyright 2017-2023 ObjectBox Ltd. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Note that this license applies to the code in this repository only.
See our website on details about all [licenses for ObjectBox components](https://objectbox.io/faq/#license-pricing).
