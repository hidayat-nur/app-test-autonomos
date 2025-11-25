package com.appautomation.presentation.viewmodel;

import com.appautomation.service.AppLauncher;
import com.appautomation.service.AutomationManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppSelectionViewModel_Factory implements Factory<AppSelectionViewModel> {
  private final Provider<AppLauncher> appLauncherProvider;

  private final Provider<AutomationManager> automationManagerProvider;

  public AppSelectionViewModel_Factory(Provider<AppLauncher> appLauncherProvider,
      Provider<AutomationManager> automationManagerProvider) {
    this.appLauncherProvider = appLauncherProvider;
    this.automationManagerProvider = automationManagerProvider;
  }

  @Override
  public AppSelectionViewModel get() {
    return newInstance(appLauncherProvider.get(), automationManagerProvider.get());
  }

  public static AppSelectionViewModel_Factory create(Provider<AppLauncher> appLauncherProvider,
      Provider<AutomationManager> automationManagerProvider) {
    return new AppSelectionViewModel_Factory(appLauncherProvider, automationManagerProvider);
  }

  public static AppSelectionViewModel newInstance(AppLauncher appLauncher,
      AutomationManager automationManager) {
    return new AppSelectionViewModel(appLauncher, automationManager);
  }
}
