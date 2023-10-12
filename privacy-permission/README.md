# 隐私权限

## 设计思路

1. 整理隐私权限涉及的代码(类名及其属性名或方法名)
2. 基于apkanalyzer收集方法堆栈, 检测是否存在"隐私权限涉及的代码"
3. 使用POI生成Excel
4. 每个隐私权限涉及的代码调用堆栈存放在一个Excel中

### apkanalyzer

```shell
Subject must be one of: apk, files, manifest, dex, resources

apk summary              Prints the application Id, version code and version name.
apk file-size            Prints the file size of the APK.
apk download-size        Prints an estimate of the download size of the APK.
apk features             Prints features used by the APK.
apk compare              Compares the sizes of two APKs.
files list               Lists all files in the zip.
files cat                Prints the given file contents to stdout
manifest print           Prints the manifest in XML format
manifest application-id  Prints the application id.
manifest version-name    Prints the version name.
manifest version-code    Prints the version code.
manifest min-sdk         Prints the minimum sdk.
manifest target-sdk      Prints the target sdk
manifest permissions     Prints a list of used permissions
manifest debuggable      Prints if the app is debuggable
dex list                 Prints a list of dex files in the APK
dex references           Prints number of references in dex files
dex packages             Prints the class tree from DEX.
P,C,M,F: indicates
                           packages, classes methods, fields
x,k,r,d: indicates
                           removed, kept, referenced and defined nodes
dex code                 Prints the bytecode of a class or method in smali format
dex reference-tree       Prints a reference tree to a given or a list of
                           classes/methods/fields.
resources packages       Prints a list of packages in resources table
resources configs        Prints a list of configurations for a type
resources value          Prints value of the given resource
resources names          Prints a list of resource names for a type
resources xml            Prints the human readable form of a binary XML

Usage:
apkanalyzer [global options] <subject> <verb> [options] <apk> [<apk2>]

Option            Description
------            -----------
--human-readable  Print sizes in human readable format
```

#### dex

```shell
Verb must be one of: list, references, packages, code, reference-tree

==============================
dex list:
Prints a list of dex files in the APK

No options specified

==============================
dex references:
Prints number of references in dex files

Option   Description
------   -----------
--files  Dex file names to include. Default: all dex files.

==============================
dex packages:
Prints the class tree from DEX.
P,C,M,F: indicates packages, classes methods, fields
x,k,r,d: indicates removed, kept, referenced and defined nodes

Option                      Description
------                      -----------
--defined-only              Only include classes defined in the APK in the output.
--files                     Dex file names to include. Default: all dex files.
--proguard-folder <File>    The Proguard output folder to search for mappings.
--proguard-mappings <File>  The Proguard mappings file.
--proguard-seeds <File>     The Proguard seeds file.
--proguard-usages <File>    The Proguard usages file.
--show-removed              Show classes and members that were removed by Proguard.

==============================
dex code:
Prints the bytecode of a class or method in smali format

Option (* = required)       Description
---------------------       -----------
* --class                   Fully qualified class name to decompile.
--method                    Method to decompile. Format: name(params)returnType, e.g.
                              someMethod(Ljava/lang/String;I)V
--proguard-folder <File>    The Proguard output folder to search for mappings.
--proguard-mappings <File>  The Proguard mappings file.

==============================
dex reference-tree:
Prints a reference tree to a given or a list of classes/methods/fields.

Option                      Description
------                      -----------
--files                     Dex file names to include. Default: all dex files.
--input-file <File>         The file with a class, method or field to query in each
                              line.
--proguard-folder <File>    The Proguard output folder to search for mappings.
--proguard-mappings <File>  The Proguard mappings file.
--proguard-seeds <File>     The Proguard seeds file.
--proguard-usages <File>    The Proguard usages file.
--references-to             Class/constructor/method/field descriptor. Format:
                              Class: class_name.
  Constructor: class_name
                              constructor_name
  Method: class_name return_type
                              method_name
  Field: class_name field_type filed_name
                              The descriptor can be copied from the output of
 .
                              /apkanalyzer dex packages

Usage:
apkanalyzer [global options] <subject> <verb> [options] <apk> [<apk2>]

Option            Description
------            -----------
--human-readable  Print sizes in human readable format
```

## 参考资料

- [实现Gradle插件](https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html)
- [懒加载属性](https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_properties)
- [ASM](https://asm.ow2.io/)
- [POI Excel](https://poi.apache.org/components/spreadsheet/index.html)
