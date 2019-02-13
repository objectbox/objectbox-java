<img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png">

# ObjectBox Java (Kotlin, Android)
ObjectBox is a superfast object-oriented database with strong relation support.
ObjectBox is embedded into your Android, Linux, macOS, or Windows app.

**Latest version: [2.3.3 (2019/02/13)](https://objectbox.io/changelog)**

Demo code using ObjectBox:

```java
Playlist playlist = new Playlist("My Favorties");
playlist.songs.add(new Song("Lalala"));
playlist.songs.add(new Song("Lololo"));
box.put(playlist);
```

Other languages/bindings
------------------------
ObjectBox supports multiple platforms and languages.
Besides JVM based languages like Java and Kotlin, ObjectBox also offers: 

* [ObjectBox Swift](https://github.com/objectbox/objectbox-swift): build fast mobile apps for iOS (and macOS) 
* [ObjectBox Go](https://github.com/objectbox/objectbox-go): great for data-driven tools and small server applications 
* [ObjectBox C API](https://github.com/objectbox/objectbox-c): native speed with zero copy access to FlatBuffer objects

Gradle setup
------------
Add this to your root build.gradle (project level): 

```groovy
buildscript {
    ext.objectboxVersion = '2.3.3'
    dependencies {
        classpath "io.objectbox:objectbox-gradle-plugin:$objectboxVersion"
    }
}
```

And this to our app's build.gradle (module level):

```groovy
apply plugin: 'io.objectbox' // after applying Android plugin
```

First steps
-----------
Create data object class `@Entity`, for example "Playlist".
```java
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

For details please check the [docs](http://objectbox.io/documentation/).     

Links
-----
[Features](https://objectbox.io/features/)

[Docs & Changelog](https://docs.objectbox.io/)

[Examples](https://github.com/objectbox/objectbox-examples)


We love to get your feedback
----------------------------
Let us know how we are doing: [2 minute questionnaire](https://docs.google.com/forms/d/e/1FAIpQLSe_fq-FlBThK_96bkHv1oEDizoHwEu_b6M4FJkMv9V5q_Or9g/viewform?usp=sf_link).
Thanks!


License
-------
    Copyright 2017-2019 ObjectBox Ltd. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

