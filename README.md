<img width="466" src="https://raw.githubusercontent.com/objectbox/objectbox-java/master/logo.png">

# ObjectBox Java (Kotlin, Android)
ObjectBox is a superfast object-oriented database with strong relation support. ObjectBox is embedded into your Android, Linux, macOS, or Windows app.

**Latest version: [2.0.0 (2018/07/25)](http://objectbox.io/changelog)**

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
    ext.objectboxVersion = '2.0.0'
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
    Copyright 2017-2018 ObjectBox Ltd. All rights reserved.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

