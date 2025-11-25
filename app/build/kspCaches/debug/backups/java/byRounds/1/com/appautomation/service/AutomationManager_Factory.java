package com.appautomation.service;

import com.appautomation.data.repository.AppRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AutomationManager_Factory implements Factory<AutomationManager> {
  private final Provider<AppLauncher> appLauncherProvider;

  private final Provider<AppMonitor> appMonitorProvider;

  private final Provider<AppRepository> repositoryProvider;

  public AutomationManager_Factory(Provider<AppLauncher> appLauncherProvider,
      Provider<AppMonitor> appMonitorProvider, Provider<AppRepository> repositoryProvider) {
    this.appLauncherProvider = appLauncherProvider;
    this.appMonitorProvider = appMonitorProvider;
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AutomationManager get() {
    return newInstance(appLauncherProvider.get(), appMonitorProvider.get(), repositoryProvider.get());
  }

  public static AutomationManager_Factory create(Provider<AppLauncher> appLauncherProvider,
      Provider<AppMonitor> appMonitorProvider, Provider<AppRepository> repositoryProvider) {
    return new AutomationManager_Factory(appLauncherProvider, appMonitorProvider, repositoryProvider);
  }

  public static AutomationManager newInstance(AppLauncher appLauncher, AppMonitor appMonitor,
      AppRepository repository) {
    return new AutomationManager(appLauncher, appMonitor, repository);
  }
}
