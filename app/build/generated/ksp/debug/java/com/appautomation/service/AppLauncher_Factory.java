package com.appautomation.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AppLauncher_Factory implements Factory<AppLauncher> {
  private final Provider<Context> contextProvider;

  public AppLauncher_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppLauncher get() {
    return newInstance(contextProvider.get());
  }

  public static AppLauncher_Factory create(Provider<Context> contextProvider) {
    return new AppLauncher_Factory(contextProvider);
  }

  public static AppLauncher newInstance(Context context) {
    return new AppLauncher(context);
  }
}
