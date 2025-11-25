package com.appautomation.presentation.viewmodel;

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
public final class MonitoringViewModel_Factory implements Factory<MonitoringViewModel> {
  private final Provider<AutomationManager> automationManagerProvider;

  public MonitoringViewModel_Factory(Provider<AutomationManager> automationManagerProvider) {
    this.automationManagerProvider = automationManagerProvider;
  }

  @Override
  public MonitoringViewModel get() {
    return newInstance(automationManagerProvider.get());
  }

  public static MonitoringViewModel_Factory create(
      Provider<AutomationManager> automationManagerProvider) {
    return new MonitoringViewModel_Factory(automationManagerProvider);
  }

  public static MonitoringViewModel newInstance(AutomationManager automationManager) {
    return new MonitoringViewModel(automationManager);
  }
}
