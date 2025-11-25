package com.appautomation.data.repository;

import com.appautomation.data.local.AutomationDao;
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
public final class AppRepository_Factory implements Factory<AppRepository> {
  private final Provider<AutomationDao> automationDaoProvider;

  public AppRepository_Factory(Provider<AutomationDao> automationDaoProvider) {
    this.automationDaoProvider = automationDaoProvider;
  }

  @Override
  public AppRepository get() {
    return newInstance(automationDaoProvider.get());
  }

  public static AppRepository_Factory create(Provider<AutomationDao> automationDaoProvider) {
    return new AppRepository_Factory(automationDaoProvider);
  }

  public static AppRepository newInstance(AutomationDao automationDao) {
    return new AppRepository(automationDao);
  }
}
