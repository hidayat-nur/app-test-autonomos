package com.appautomation.di;

import android.content.Context;
import com.appautomation.service.AppMonitor;
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
public final class AppModule_ProvideAppMonitorFactory implements Factory<AppMonitor> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideAppMonitorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppMonitor get() {
    return provideAppMonitor(contextProvider.get());
  }

  public static AppModule_ProvideAppMonitorFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideAppMonitorFactory(contextProvider);
  }

  public static AppMonitor provideAppMonitor(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAppMonitor(context));
  }
}
