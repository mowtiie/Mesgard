# SQLCipher calls back into these classes from native code.
-keep class net.zetetic.database.** { *; }

# Stack traces from release builds stay readable with the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
