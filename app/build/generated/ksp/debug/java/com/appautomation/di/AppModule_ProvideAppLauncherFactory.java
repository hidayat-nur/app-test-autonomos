package com.appautomation.di;

import android.content.Context;
import com.appautomation.service.AppLauncher;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAppLauncherFactory implements Factory<AppLauncher> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideAppLauncherFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppLauncher get() {
    return provideAppLauncher(contextProvider.get());
  }

  public static AppModule_ProvideAppLauncherFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideAppLauncherFactory(contextProvider);
  }

  public static AppLauncher provideAppLauncher(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAppLauncher(context));
  }
}
