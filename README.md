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

## Table of Contents

- [Key Features](#key-features)
- [Getting Started](#getting-started)
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

## Getting Started

> [!NOTE]
> Prefer to look at example code? Check out [our examples repository](https://github.com/objectbox/objectbox-examples).

You can add the ObjectBox Java SDK using a:

- [Gradle setup](#gradle-setup)
- [Maven setup](#maven-setup)

ObjectBox tools and dependencies are available on [the Maven Central repository](https://central.sonatype.com/namespace/io.objectbox).

The database libraries available for the ObjectBox Java SDK support:

- JVM 8 or newer
  - Linux (x64, arm64, armv7)
  - macOS (x64, arm64)
  - Windows (x64)
- Android 5.0 (API level 21) or newer

The ObjectBox Java SDK supports:

- Java 8 or newer
- Kotlin 1.7 or newer

The [ObjectBox Gradle plugin](https://github.com/objectbox/objectbox-java-generator) supports:

- Gradle 7.0 or newer
- Android Gradle Plugin 8.1 or newer
- JDK 11 or newer

### Gradle setup

For Gradle projects, add the required plugins to your root Gradle script.

When using a [TOML version catalog](https://docs.gradle.org/current/userguide/version_catalogs.html) and plugins syntax 
(for alternatives see below):
          
```toml
# gradle/libs.versions.toml

[versions]
# Define a variable for the version of the plugin
objectbox = "5.2.0"

# For an Android project
agp = "<AGP_VERSION>"

# If using Kotlin
kotlin = "<KOTLIN_VERSION>"

[plugins]
# Add an alias for the plugin
objectbox = { id = "io.objectbox", version.ref = "objectbox" }

# For an Android project, using Android Gradle Plugin 9.0 or newer
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-kapt = { id = "com.android.legacy-kapt", version.ref = "agp" }

# For an Android project, using Android Gradle Plugin 8.13 or older
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }

# For a JVM project, if using Kotlin 
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

```kotlin
// build.gradle.kts

plugins {
    // Add the plugin
    alias(libs.plugins.objectbox) apply false

    // For an Android project, using Android Gradle Plugin 9.0 or newer
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.kapt) apply false  
  
    // For an Android project, using Android Gradle Plugin 8.13 or older
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false

    // For a JVM project, if using Kotlin
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

allprojects {
    repositories {
        // Add Maven Central to the dependency repositories
        mavenCentral()
    }
}
```

```kotlin
// settings.gradle.kts

pluginManagement {
    repositories {
        // Add Maven Central to the plugin repositories
        mavenCentral()
    }
    
    resolutionStrategy {
        eachPlugin {
            // Map the plugin ID to the Maven artifact
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
```

Then, in the Gradle script of your subproject apply the necessary plugins:

```kotlin
// app/build.gradle.kts

plugins {
    // For an Android project, using Android Gradle Plugin 9.0 or newer
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.kapt)
    
    // For an Android project, using Android Gradle Plugin 8.13 or older
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    
    // For a JVM project
    id("application") // or id("java-library")
    // Optional, if using Kotlin
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)

    // Finally, apply the plugin
    alias(libs.plugins.objectbox)
}
```

Finally, sync the Gradle project with your IDE (for ex. using "Sync Project with Gradle Files" in Android Studio).

Your project can now use ObjectBox, continue by [defining entity classes](https://docs.objectbox.io/getting-started#define-entity-classes).

#### Alternatives

<details><summary>Using plugins syntax with plugin IDs</summary>

```kotlin
// build.gradle.kts

plugins {
    // Add the plugin
    id("io.objectbox") version "5.2.0" apply false
}

allprojects {
    repositories {
        // Add Maven Central to the dependency repositories
        mavenCentral()
    }
}
```

```kotlin
// settings.gradle.kts

pluginManagement {
    repositories {
        // Add Maven Central to the plugin repositories
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            // Map the plugin ID to the Maven artifact
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
```

</details>

<details><summary>Using buildscript syntax (KTS)</summary>

```kotlin
// build.gradle.kts

buildscript {
    // Define a variable for the plugin version
    val objectboxVersion by extra("5.2.0")
  
    repositories {
        // Add Maven Central to the plugin repositories     
        mavenCentral()    
    }
  
    dependencies {
        // Add the plugin
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}

allprojects {
    repositories {
        // Add Maven Central to the dependency repositories
        mavenCentral()
    }
}
```

</details>

<details><summary>Using buildscript syntax (Groovy)</summary>

```groovy
// build.gradle

buildscript {
    // Define a variable for the plugin version
    ext.objectboxVersion = "5.2.0"
  
    repositories {        
        // Add Maven Central to the plugin repositories
        mavenCentral()    
    }
  
    dependencies {
        // Add the plugin
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}

allprojects {
    repositories {
        // Add Maven Central to the dependency repositories
        mavenCentral()
    }
}
```

</details>

Then, in the Gradle script of your subproject apply the necessary plugins using their IDs:

```kotlin
// app/build.gradle.kts

plugins {
    // For an Android project, using Android Gradle Plugin 9.0 or newer
    id("com.android.application") // or id("com.android.library")
    id("com.android.legacy-kapt") 
  
    // For an Android project, using Android Gradle Plugin 8.13 or older
    id("com.android.application") // or id("com.android.library")
    id("org.jetbrains.kotlin.android") // or kotlin("android")
    id("org.jetbrains.kotlin.kapt") // or kotlin("kapt")  
  
    // For a JVM project
    id("application") // or id("java-library")
    // Optional, if using Kotlin
    id("org.jetbrains.kotlin.jvm") // or kotlin("jvm")
    id("org.jetbrains.kotlin.kapt") // or kotlin("kapt")  

    // Finally, apply the plugin
    id("io.objectbox")
}
```

### Maven setup

This is currently only supported for JVM projects.

To set up a Maven project, see the [README of the Java Maven example project](https://github.com/objectbox/objectbox-examples/blob/main/java-main-maven/README.md).

## Frequently Asked Questions and Troubleshooting

If you encounter any problems, check out the [FAQ](https://docs.objectbox.io/faq) and [Troubleshooting](https://docs.objectbox.io/troubleshooting) pages.

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
