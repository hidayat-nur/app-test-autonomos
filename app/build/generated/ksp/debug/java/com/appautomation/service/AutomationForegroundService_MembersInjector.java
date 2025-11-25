package com.appautomation.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AutomationForegroundService_MembersInjector implements MembersInjector<AutomationForegroundService> {
  private final Provider<AutomationManager> automationManagerProvider;

  public AutomationForegroundService_MembersInjector(
      Provider<AutomationManager> automationManagerProvider) {
    this.automationManagerProvider = automationManagerProvider;
  }

  public static MembersInjector<AutomationForegroundService> create(
      Provider<AutomationManager> automationManagerProvider) {
    return new AutomationForegroundService_MembersInjector(automationManagerProvider);
  }

  @Override
  public void injectMembers(AutomationForegroundService instance) {
    injectAutomationManager(instance, automationManagerProvider.get());
  }

  @InjectedFieldSignature("com.appautomation.service.AutomationForegroundService.automationManager")
  public static void injectAutomationManager(AutomationForegroundService instance,
      AutomationManager automationManager) {
    instance.automationManager = automationManager;
  }
}
