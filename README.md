<img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png">

# ObjectBox Java (Kotlin, Android)
ObjectBox is a superfast object-oriented database with strong relation support.

**Latest version: [1.3.4 (2017/12/07)](http://objectbox.io/changelog)**

Demo code using ObjectBox:

```java
Playlist playlist = new Playlist("My Favorties");
playlist.songs.add(new Song("Lalala"));
playlist.songs.add(new Song("Lololo"));
box.put(playlist);
```

Gradle setup
------------
Add this to your root build.gradle (project level): 

```groovy
buildscript {
    ext.objectboxVersion = '1.3.4'
    repositories {
        maven { url "http://objectbox.net/beta-repo/" }
    }
    dependencies {
        classpath "io.objectbox:objectbox-gradle-plugin:$objectboxVersion"
    }
    
}
    
allprojects {
    repositories {
        maven { url "http://objectbox.net/beta-repo/" }
    }
}
```

And this to our app's build.gradle (module level):

```groovy
apply plugin: 'io.objectbox' // after applying Android plugin
```

First steps
-----------
Prepare the BoxStore object once for your app, e.g. in `onCreate` in your Application class:

```java
boxStore = MyObjectBox.builder().androidContext(this).build();
```

Create data object class `@Entity`, for example "Playlist".
Then get a `Box` class for this entity class:

```java
Box<Playlist> box = boxStore.boxFor(Playlist.class);
```

The `Box` object gives you access to all major functions, like `put`, `get`, `remove`, and `query`.

For details please check the [docs](http://objectbox.io/documentation/).     

Links
-----
[Features](http://objectbox.io/features/)

[Documentation](http://objectbox.io/documentation/)

[Examples](https://github.com/objectbox/objectbox-examples)

[Changelog](http://objectbox.io/changelog/)

We love to get your feedback
----------------------------
Let us know how we are doing: [2 minute questionnaire](https://docs.google.com/forms/d/e/1FAIpQLSe_fq-FlBThK_96bkHv1oEDizoHwEu_b6M4FJkMv9V5q_Or9g/viewform?usp=sf_link).
Thanks!


License
-------
    Copyright 2017 ObjectBox Ltd. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

