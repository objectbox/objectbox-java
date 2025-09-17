<p align="center"><img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png" alt="ObjectBox"></p>

<p align="center">
  <a href="https://docs.objectbox.io/getting-started">Getting Started</a> •
  <a href="https://docs.objectbox.io">Documentation</a> •
  <a href="https://github.com/objectbox/objectbox-examples">Example Apps</a> •
  <a href="https://github.com/objectbox/objectbox-java/issues">Issues</a>
</p>

<p align="center">
  <a href="https://github.com/objectbox/objectbox-java/releases/latest">
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

# ObjectBox - Fast and Efficient Java Database (Android, JVM) with Vector Search

ObjectBox Java is a lightweight yet powerful on-device database & vector database designed specifically for **Java and Kotlin** applications.
Store and manage data effortlessly in your Android or JVM Linux, macOS or Windows app with ObjectBox.
Easily manage vector data alongside your objects and perform superfast on-device vector search to empower your apps with RAG AI, generative AI, and similarity search.
Enjoy exceptional speed, battery-friendly resource usage, and environmentally-friendly development. 💚

ObjectBox provides a store with boxes to put objects into:

#### JVM + Java example

```java
// Annotate a class to create a Box
@Entity
public class Person {
  private @Id long id;
  private String firstName;
  private String lastName;

  // Constructor, getters and setters left out for simplicity
}
 
BoxStore store = MyObjectBox.builder()
        .name("person-db")
        .build();
 
Box<Person> box = store.boxFor(Person.class);
 
Person person = new Person("Joe", "Green");
long id = box.put(person);    // Create
person = box.get(id);         // Read
person.setLastName("Black");
box.put(person);              // Update
box.remove(person);           // Delete
```

#### Android + Kotlin example

```kotlin
// Annotate a class to create a Box
@Entity
data class Person(
    @Id var id: Long = 0,
    var firstName: String? = null,
    var lastName: String? = null
)
 
val store = MyObjectBox.builder()
                .androidContext(context)
                .build()
 
val box = store.boxFor(Person::class)
 
var person = Person(firstName = "Joe", lastName = "Green")
val id = box.put()   // Create
person = box.get(id) // Read
person.lastName = "Black"
box.put(person)     // Update
box.remove(person)  // Delete
```

Continue with the ➡️ **[Getting Started guide](https://docs.objectbox.io/getting-started)**.

## Table of Contents

- [Key Features](#key-features)
- [Getting started](#getting-started)
  - [Gradle setup](#gradle-setup)
  - [Maven setup](#maven-setup)
- [Why use ObjectBox?](#why-use-objectbox-for-java-data-management)
- [Community and Support](#community-and-support)
- [Changelog](#changelog)
- [Other languages/bindings](#other-languagesbindings)
- [License](#license)

## Key Features

🧠 **First on-device vector database:** easily manage vector data and perform fast vector search
🏁 **High performance:** exceptional speed, outperforming alternatives like SQLite and Realm in all CRUD operations.\
💚 **Efficient Resource Usage:** minimal CPU, power and memory consumption for maximum flexibility and sustainability.\
🔗 **[Built-in Object Relations](https://docs.objectbox.io/relations):** built-in support for object relations, allowing you to easily establish and manage relationships between objects.\
👌 **Ease of use:** concise API that eliminates the need for complex SQL queries, saving you time and effort during development.

## Getting started

### Gradle setup

For Gradle projects, add the ObjectBox Gradle plugin to your root Gradle script:

```kotlin
// build.gradle.kts
buildscript {
    val objectboxVersion by extra("5.0.0")
    repositories {        
        mavenCentral()    
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}
```

<details><summary>Using plugins syntax</summary>

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.0.2" apply false // When used in an Android project
    id("io.objectbox") version "5.0.0" apply false
}
```

```kotlin
// settings.gradle.kts
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
```

</details>

<details><summary>Using Groovy syntax</summary>

```groovy
// build.gradle
buildscript {
    ext.objectboxVersion = "5.0.0"
    repositories {        
        mavenCentral()    
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}
```

</details>

And in the Gradle script of your subproject apply the plugin:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application") // When used in an Android project
    kotlin("android") // When used in an Android project
    kotlin("kapt")
    id("io.objectbox") // Add after other plugins
}
```

Then sync the Gradle project with your IDE.

Your project can now use ObjectBox, continue by [defining entity classes](https://docs.objectbox.io/getting-started#define-entity-classes).

### Maven setup

This is currently only supported for JVM projects.

To set up a Maven project, see the [README of the Java Maven example project](https://github.com/objectbox/objectbox-examples/blob/main/java-main-maven/README.md).

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
- Drop us a line via contact[at]objectbox.io
- ⭐ us on GitHub if you like what you see!

Thank you! Stay updated with our [blog](https://objectbox.io/blog).

## Changelog

For notable and important changes in new releases, read the [changelog](CHANGELOG.md).

## Other languages/bindings

ObjectBox supports multiple platforms and languages.
Besides JVM based languages like Java and Kotlin, ObjectBox also offers:

- [C and C++ SDK](https://github.com/objectbox/objectbox-c): native speed with zero copy access to FlatBuffer objects
- [Dart and Flutter SDK](https://github.com/objectbox/objectbox-dart): cross-platform for mobile and desktop apps
- [Go SDK](https://github.com/objectbox/objectbox-go): great for data-driven tools and embedded server applications
- [Swift SDK](https://github.com/objectbox/objectbox-swift): build fast mobile apps for iOS (and macOS)

## License

```text
Copyright 2017-2025 ObjectBox Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Note that this license applies to the code in this repository only.
See our website on details about all [licenses for ObjectBox components](https://objectbox.io/faq/#license-pricing).
