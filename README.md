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

# ObjectBox - Fast and Efficient Java Database (Android, JVM)

ObjectBox Java is a simple yet powerful database designed specifically for **Java and Kotlin** applications.
Store and manage data effortlessly in your Android or JVM Linux, macOS or Windows app with ObjectBox.
Enjoy exceptional speed, frugal resource usage, and environmentally-friendly development. 💚

### Demo code

```java
// Java
Playlist playlist = new Playlist("My Favorites");
playlist.songs.add(new Song("Lalala"));
playlist.songs.add(new Song("Lololo"));
box.put(playlist);
```

➡️ [More details in the docs](https://docs.objectbox.io/)

```kotlin
// Kotlin
val playlist = Playlist("My Favorites")
playlist.songs.add(Song("Lalala"))
playlist.songs.add(Song("Lololo"))
box.put(playlist)
```

## Table of Contents
- [Key Features](#key-features)
- [Getting started](#getting-started)
  - [Gradle setup](#gradle-setup)
  - [First steps](#first-steps)
- [Why use ObjectBox?](#why-use-objectbox-for-java-data-management)
- [Community and Support](#community-and-support)
- [Other languages/bindings](#other-languagesbindings)
- [License](#license)

## Key Features
🏁 **High performance:** exceptional speed, outperforming alternatives like SQLite and Realm in all CRUD operations.\
💚 **Efficient Resource Usage:** minimal CPU, power and memory consumption for maximum flexibility and sustainability.\
🔗 **[Built-in Object Relations](https://docs.objectbox.io/relations):** built-in support for object relations, allowing you to easily establish and manage relationships between objects.\
👌 **Ease of use:** concise API that eliminates the need for complex SQL queries, saving you time and effort during development.

## Getting started
### Gradle setup

For Android projects, add the ObjectBox Gradle plugin to your root `build.gradle`: 

```groovy
buildscript {
    ext.objectboxVersion = "3.8.0"
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

## Why use ObjectBox for Java data management?

ObjectBox is a NoSQL Java database designed for local data storage on resource-restricted devices, prioritizing 
offline-first functionality. It is a smart and sustainable choice for local data persistence in Java and Kotlin 
applications. It offers efficiency, ease of use, and flexibility.

### Fast but resourceful
Optimized for speed and minimal resource consumption, ObjectBox is an ideal solution for mobile devices. It has 
excellent performance, while also minimizing CPU, RAM, and power usage. ObjectBox outperforms SQLite and Realm across 
all CRUD (Create, Read, Update, Delete) operations. Check out our [Performance Benchmarking App repository](https://github.com/objectbox/objectbox-performance).

### Simple but powerful
With its concise language-native API, ObjectBox simplifies development by requiring less code compared to SQLite. It 
operates on plain objects (POJOs) with built-in relations, eliminating the need to manage rows and columns. This 
approach is efficient for handling large data volumes and allows for easy model modifications.

### Functionality

💐 **[Queries](https://docs.objectbox.io/queries):** filter data as needed, even across relations\
💻 **[Multiplatform](https://docs.objectbox.io/faq#on-which-platforms-does-objectbox-run):** supports Android and JVM on Linux (also on ARM), Windows and macOS\
🌱 **Scalable:** handling millions of objects resource-efficiently with ease\
🦮 **Statically typed:** compile time checks & optimizations\
📃 **Automatic schema migrations:** no update scripts needed

**And much more than just data persistence**\
🔄 **[ObjectBox Sync](https://objectbox.io/sync/):** keeps data in sync between devices and servers\
🕒 **[ObjectBox TS](https://objectbox.io/time-series-database/):** time series extension for time based data

## Community and Support

❤ **Tell us what you think!** Share your thoughts through our [Anonymous Feedback Form](https://forms.gle/bdktGBUmL4m48ruj7).

At ObjectBox, we are dedicated to bringing joy and delight to app developers by providing intuitive and fun-to-code-with
APIs. We genuinely want to hear from you: What do you love about ObjectBox? What could be improved? Where do you face
challenges in everyday app development?

**We eagerly await your comments and requests, so please feel free to reach out to us:**
- Add [GitHub issues](https://github.com/ObjectBox/objectbox-java/issues) 
- Upvote important issues 👍
- Drop us a line via [@ObjectBox_io](https://twitter.com/ObjectBox_io/) or contact[at]objectbox.io
- ⭐ us on GitHub if you like what you see! 

Thank you! Stay updated with our [blog](https://objectbox.io/blog).

## Other languages/bindings

ObjectBox supports multiple platforms and languages.
Besides JVM based languages like Java and Kotlin, ObjectBox also offers: 

* [Swift Database](https://github.com/objectbox/objectbox-swift): build fast mobile apps for iOS (and macOS) 
* [Dart/Flutter Database](https://github.com/objectbox/objectbox-dart): cross-platform for mobile and desktop apps 
* [Go Database](https://github.com/objectbox/objectbox-go): great for data-driven tools and embedded server applications 
* [C and C++ Database](https://github.com/objectbox/objectbox-c): native speed with zero copy access to FlatBuffer objects


## License

    Copyright 2017-2024 ObjectBox Ltd. All rights reserved.
    
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
