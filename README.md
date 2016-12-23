## Tinker的使用笔记
记录一下接入Tinker的基本步骤和心得
## 第一步 配置gradle
在主项目的`build.gradle`中配置
```gradle
buildscript {
    dependencies {
        classpath ('com.tencent.tinker:tinker-patch-gradle-plugin:1.7.5')
    }
}
```
在主model下的`build.gradle`配置
```gradle
dependencies {
    //optional, help to generate the final application 
    provided('com.tencent.tinker:tinker-android-anno:1.7.5')
    //tinker's main Android lib
    compile('com.tencent.tinker:tinker-android-lib:1.7.5') 
}
...
...
apply plugin: 'com.tencent.tinker.patch'
```
可以参考官方的配置[app/build.gradle](https://github.com/Tencent/tinker/blob/master/tinker-sample-android/app/build.gradle).
也可以直接复制以下配置，相比于官方的配置，修改了tinkerId的获取方式，由versionName来获得，所以在生成差异包时需要注意修改versionName
```gradle
apply plugin: 'com.android.application'



dependencies {
  compile fileTree(dir: 'libs', include: ['*.jar'])
  testCompile 'junit:junit:4.12'
  compile "com.android.support:appcompat-v7:23.1.1"
  //可选，用于生成application类
  provided('com.tencent.tinker:tinker-android-anno:1.7.5')
  //tinker的核心库
  compile('com.tencent.tinker:tinker-android-lib:1.7.5')

  compile "com.android.support:multidex:1.0.1"
  androidTestCompile('com.android.support.test.espresso:espresso-core:2.2.2', {
    exclude group: 'com.android.support', module: 'support-annotations'
  })
  testCompile 'junit:junit:4.12'
  //use to test multiDex
  //    compile group: 'com.google.guava', name: 'guava', version: '19.0'
  //    compile "org.scala-lang:scala-library:2.11.7"

  //use for local maven test
  //    compile("com.tencent.tinker:tinker-android-loader:${TINKER_VERSION}") { changing = true }
  //    compile("com.tencent.tinker:aosp-dexutils:${TINKER_VERSION}") { changing = true }
  //    compile("com.tencent.tinker:bsdiff-util:${TINKER_VERSION}") { changing = true }
  //    compile("com.tencent.tinker:tinker-commons:${TINKER_VERSION}") { changing = true }

}
def javaVersion = JavaVersion.VERSION_1_7

android {
  compileSdkVersion 23
  buildToolsVersion "23.0.2"

  compileOptions {
    sourceCompatibility javaVersion
    targetCompatibility javaVersion
  }
  //recommend
  dexOptions {
    jumboMode = true
  }

  signingConfigs {
    release {
      try {
        storeFile file("./keystore/release.keystore")
        storePassword "testres"
        keyAlias "testres"
        keyPassword "testres"
      } catch (ex) {
        throw new InvalidUserDataException(ex.toString())
      }
    }

  }

  defaultConfig {
    applicationId "tinker.sample.android"
    minSdkVersion 10
    targetSdkVersion 22
    versionCode 1
    versionName "1.0.0"
    /**
     * you can use multiDex and install it in your ApplicationLifeCycle implement
     */
    multiDexEnabled true
    /**
     * not like proguard, multiDexKeepProguard is not a list, so we can't just
     * add for you in our task. you can copy tinker keep rules at
     * build/intermediates/tinker_intermediates/tinker_multidexkeep.pro
     */
    multiDexKeepProguard file("keep_in_main_dex.txt")
    /**
     * buildConfig can change during patch!
     * we can use the newly value when patch
     */
    buildConfigField "String", "MESSAGE", "\"I am the base apk\""
    //        buildConfigField "String", "MESSAGE", "\"I am the patch apk\""
    /**
     * client version would update with patch
     * so we can get the newly git version easily!
     */
    buildConfigField "String", "TINKER_ID", "\"${getTinkerIdValue()}\""
    buildConfigField "String", "PLATFORM",  "\"all\""
  }

  //    aaptOptions{
  //        cruncherEnabled false
  //    }

  //    //use to test flavors support
  //    productFlavors {
  //        flavor1 {
  //            applicationId 'tinker.sample.android.flavor1'
  //        }
  //
  //        flavor2 {
  //            applicationId 'tinker.sample.android.flavor2'
  //        }
  //    }

  buildTypes {
    release {
      minifyEnabled true
      signingConfig signingConfigs.release
      proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
    }
    debug {
      debuggable true
      minifyEnabled false
      signingConfig signingConfigs.debug
    }
  }
  sourceSets {
    main {
      jniLibs.srcDirs = ['libs']
    }
  }
}

def bakPath = file("${buildDir}/bakApk/")

/**
 * you can use assembleRelease to build you base apk
 * use tinkerPatchRelease -POLD_APK=  -PAPPLY_MAPPING=  -PAPPLY_RESOURCE= to build patch
 * add apk from the build/bakApk
 */
ext {
  //for some reason, you may want to ignore tinkerBuild, such as instant run debug build?
  tinkerEnabled = true

  //for normal build
  //old apk file to build patch apk
  tinkerOldApkPath = "${bakPath}/app-debug-1223-14-52-30.apk"
  //proguard mapping file to build patch apk
  tinkerApplyMappingPath = "${bakPath}/app-debug-1018-17-32-47-mapping.txt"
  //resource R.txt to build patch apk, must input if there is resource changed
  tinkerApplyResourcePath = "${bakPath}/app-debug-1223-14-52-30-R.txt"

  //only use for build all flavor, if not, just ignore this field
  tinkerBuildFlavorDirectory = "${bakPath}/app-1018-17-32-47"
}

def getOldApkPath() {
  return hasProperty("OLD_APK") ? OLD_APK : ext.tinkerOldApkPath
}

def getApplyMappingPath() {
  return hasProperty("APPLY_MAPPING") ? APPLY_MAPPING : ext.tinkerApplyMappingPath
}

def getApplyResourceMappingPath() {
  return hasProperty("APPLY_RESOURCE") ? APPLY_RESOURCE : ext.tinkerApplyResourcePath
}

def getTinkerIdValue() {
  //版本作为id
  return android.defaultConfig.versionName
}

def buildWithTinker() {
  return hasProperty("TINKER_ENABLE") ? TINKER_ENABLE : ext.tinkerEnabled
}

def getTinkerBuildFlavorDirectory() {
  return ext.tinkerBuildFlavorDirectory
}

if (buildWithTinker()) {
  apply plugin: 'com.tencent.tinker.patch'

  tinkerPatch {
    /**
     * necessary，default 'null'
     * the old apk path, use to diff with the new apk to build
     * add apk from the build/bakApk
     */
    oldApk = getOldApkPath()
    /**
     * optional，default 'false'
     * there are some cases we may get some warnings
     * if ignoreWarning is true, we would just assert the patch process
     * case 1: minSdkVersion is below 14, but you are using dexMode with raw.
     *         it must be crash when load.
     * case 2: newly added Android Component in AndroidManifest.xml,
     *         it must be crash when load.
     * case 3: loader classes in dex.loader{} are not keep in the main dex,
     *         it must be let tinker not work.
     * case 4: loader classes in dex.loader{} changes,
     *         loader classes is ues to load patch dex. it is useless to change them.
     *         it won't crash, but these changes can't effect. you may ignore it
     * case 5: resources.arsc has changed, but we don't use applyResourceMapping to build
     */
    ignoreWarning = false

    /**
     * optional，default 'true'
     * whether sign the patch file
     * if not, you must do yourself. otherwise it can't check success during the patch loading
     * we will use the sign config with your build type
     */
    useSign = true

    /**
     * Warning, applyMapping will affect the normal android build!
     */
    buildConfig {
      /**
       * optional，default 'null'
       * if we use tinkerPatch to build the patch apk, you'd better to apply the old
       * apk mapping file if minifyEnabled is enable!
       * Warning:
       * you must be careful that it will affect the normal assemble build!
       */
      applyMapping = getApplyMappingPath()
      /**
       * optional，default 'null'
       * It is nice to keep the resource id from R.txt file to reduce java changes
       */
      applyResourceMapping = getApplyResourceMappingPath()

      /**
       * necessary，default 'null'
       * because we don't want to check the base apk with md5 in the runtime(it is slow)
       * tinkerId is use to identify the unique base apk when the patch is tried to apply.
       * we can use git rev, svn rev or simply versionCode.
       * we will gen the tinkerId in your manifest automatic
       */
      tinkerId = getTinkerIdValue()
    }

    dex {
      /**
       * optional，default 'jar'
       * only can be 'raw' or 'jar'. for raw, we would keep its original format
       * for jar, we would repack dexes with zip format.
       * if you want to support below 14, you must use jar
       * or you want to save rom or check quicker, you can use raw mode also
       */
      dexMode = "jar"
      /**
       * optional，default 'false'
       * if usePreGeneratedPatchDex is true, tinker framework will generate auxiliary class
       * and insert auxiliary instruction when compiling base package using
       * assemble{Debug/Release} task to prevent class pre-verified issue in dvm.
       * Besides, a real dex file contains necessary class will be generated and packed into
       * patch package instead of any patch info files.
       *
       * Use this mode if you have to use any dex encryption solutions.
       *
       * Notice: If you change this value, please trigger clean task
       * and regenerate base package.
       */
      usePreGeneratedPatchDex = false
      /**
       * necessary，default '[]'
       * what dexes in apk are expected to deal with tinkerPatch
       * it support * or ? pattern.
       */
      pattern = ["classes*.dex",
                 "assets/secondary-dex-?.jar"]
      /**
       * necessary，default '[]'
       * Warning, it is very very important, loader classes can't change with patch.
       * thus, they will be removed from patch dexes.
       * you must put the following class into main dex.
       * Simply, you should add your own application {@code tinker.sample.android.SampleApplication}
       * own tinkerLoader, and the classes you use in them
       *
       */
      loader = ["com.tencent.tinker.loader.*",
                //warning, you must change it with your application
                "com.zwh.thinkertest.MyApplication",
                //use sample, let BaseBuildInfo unchangeable with tinker
                "tinker.sample.android.app.BaseBuildInfo"
      ]
    }

    lib {
      /**
       * optional，default '[]'
       * what library in apk are expected to deal with tinkerPatch
       * it support * or ? pattern.
       * for library in assets, we would just recover them in the patch directory
       * you can get them in TinkerLoadResult with Tinker
       */
      pattern = ["lib/armeabi/*.so"]
    }

    res {
      /**
       * optional，default '[]'
       * what resource in apk are expected to deal with tinkerPatch
       * it support * or ? pattern.
       * you must include all your resources in apk here,
       * otherwise, they won't repack in the new apk resources.
       */
      pattern = ["res/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]

      /**
       * optional，default '[]'
       * the resource file exclude patterns, ignore add, delete or modify resource change
       * it support * or ? pattern.
       * Warning, we can only use for files no relative with resources.arsc
       */
      ignoreChange = ["assets/sample_meta.txt"]

      /**
       * default 100kb
       * for modify resource, if it is larger than 'largeModSize'
       * we would like to use bsdiff algorithm to reduce patch file size
       */
      largeModSize = 100
    }

    packageConfig {
      /**
       * optional，default 'TINKER_ID, TINKER_ID_VALUE' 'NEW_TINKER_ID, NEW_TINKER_ID_VALUE'
       * package meta file gen. path is assets/package_meta.txt in patch file
       * you can use securityCheck.getPackageProperties() in your ownPackageCheck method
       * or TinkerLoadResult.getPackageConfigByName
       * we will get the TINKER_ID from the old apk manifest for you automatic,
       * other config files (such as patchMessage below)is not necessary
       */
      configField("patchMessage", "tinker is sample to use")
      /**
       * just a sample case, you can use such as sdkVersion, brand, channel...
       * you can parse it in the SamplePatchListener.
       * Then you can use patch conditional!
       */
      configField("platform", "all")
      /**
       * patch version via packageConfig
       */
      configField("patchVersion", "1.0")
    }
    //or you can add config filed outside, or get meta value from old apk
    //project.tinkerPatch.packageConfig.configField("test1", project.tinkerPatch.packageConfig.getMetaDataFromOldApk("Test"))
    //project.tinkerPatch.packageConfig.configField("test2", "sample")

    /**
     * if you don't use zipArtifact or path, we just use 7za to try
     */
    sevenZip {
      /**
       * optional，default '7za'
       * the 7zip artifact path, it will use the right 7za with your platform
       */
      zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
      /**
       * optional，default '7za'
       * you can specify the 7za path yourself, it will overwrite the zipArtifact value
       */
      //        path = "/usr/local/bin/7za"
    }
  }

  List<String> flavors = new ArrayList<>();
  project.android.productFlavors.each {flavor ->
    flavors.add(flavor.name)
  }
  boolean hasFlavors = flavors.size() > 0
  /**
   * bak apk and mapping
   */
  android.applicationVariants.all { variant ->
    /**
     * task type, you want to bak
     */
    def taskName = variant.name
    def date = new Date().format("MMdd-HH-mm-ss")

    tasks.all {
      if ("assemble${taskName.capitalize()}".equalsIgnoreCase(it.name)) {

        it.doLast {
          copy {
            def fileNamePrefix = "${project.name}-${variant.baseName}"
            def newFileNamePrefix = hasFlavors ? "${fileNamePrefix}" : "${fileNamePrefix}-${date}"

            def destPath = hasFlavors ? file("${bakPath}/${project.name}-${date}/${variant.flavorName}") : bakPath
            from variant.outputs.outputFile
            into destPath
            rename { String fileName ->
              fileName.replace("${fileNamePrefix}.apk", "${newFileNamePrefix}.apk")
            }

            from "${buildDir}/outputs/mapping/${variant.dirName}/mapping.txt"
            into destPath
            rename { String fileName ->
              fileName.replace("mapping.txt", "${newFileNamePrefix}-mapping.txt")
            }

            from "${buildDir}/intermediates/symbols/${variant.dirName}/R.txt"
            into destPath
            rename { String fileName ->
              fileName.replace("R.txt", "${newFileNamePrefix}-R.txt")
            }
          }
        }
      }
    }
  }
}

...
## 第二步 生成Application
自定义一个ApplecationLike继承DefaultApplicationLike，添加一个DefaultLifeCycle注解,DefaultLifeCycle注解中的com.zwh.thinkertest.MyApplication
就是项目编译自动生成的，需要在Manifest文件中添加android:name=".MyApplication"。
```java
@DefaultLifeCycle(application = "com.zwh.thinkertest.MyApplication",
    flags = ShareConstants.TINKER_ENABLE_ALL,
    loadVerifyFlag = false)
public class MyApplicationLike extends DefaultApplicationLike {
  public MyApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag,
      long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent,
      Resources[] resources, ClassLoader[] classLoader, AssetManager[] assetManager) {
    super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime,
        applicationStartMillisTime, tinkerResultIntent, resources, classLoader, assetManager);
  }

  /**
   * Tinker初始化
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onBaseContextAttached(Context base) {
    super.onBaseContextAttached(base);
    //you must install multiDex whatever tinker is installed!
    MultiDex.install(base);

    SampleApplicationContext.application = getApplication();
    SampleApplicationContext.context = getApplication();
    TinkerManager.setTinkerApplicationLike(this);
    TinkerManager.initFastCrashProtect();
    //should set before tinker is installed
    TinkerManager.setUpgradeRetryEnable(true);

    //optional set logIml, or you can use default debug log
    TinkerInstaller.setLogIml(new MyLogImp());

    //installTinker after load multiDex
    //or you can put com.tencent.tinker.** to main dex
    TinkerManager.installTinker(this);
  }
  
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
    getApplication().registerActivityLifecycleCallbacks(callback);
  }

}
```
## 第三步 添加一些初始化Tinker的工具类和日志、异常等
参考官方事例[tinker-sample-android](https://github.com/Tencent/tinker/tree/master/tinker-sample-android)把该项目下的一些工具类和日志、异常等类
复制到你自己的项目中。
## 第四步 release获取基准包
在命令行窗口中输入gradlew tinkerPatchRelease，第一次输入可能会报错，原因是没有该基准包，但不影响基准包的获取。Build结束后，可以在build/bakApk文件夹下得到基准包，
将基准报的名称复制替换build.gradle下tinkerPatch中的oldApk参数。
## 第五步 release获取差异包
修改你自己的代码后，重复第四步的操作，Build成功后，可以在build/outputs/thinkerPath/release下生成patc_signed_7zip.apk,这个就是我们要的差异包，接下来
就是在代码中执行
```java
TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), "获取差异包后保存的路径" + "/patch_signed_7zip.apk");

```
最后重启app后，就会发现热修复成功了
