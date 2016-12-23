package com.zwh.thinkertest;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.multidex.MultiDex;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.zwh.thinkertest.Log.MyLogImp;
import com.zwh.thinkertest.util.SampleApplicationContext;
import com.zwh.thinkertest.util.TinkerManager;

/**
 * Created by Zhangwh on 2016/12/23 0023.
 * email:616505546@qq.com
 */
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
