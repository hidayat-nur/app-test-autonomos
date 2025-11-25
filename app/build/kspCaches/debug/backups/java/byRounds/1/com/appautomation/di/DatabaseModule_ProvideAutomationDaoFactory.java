package com.appautomation.di;

import com.appautomation.data.local.AppDatabase;
import com.appautomation.data.local.AutomationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideAutomationDaoFactory implements Factory<AutomationDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideAutomationDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AutomationDao get() {
    return provideAutomationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideAutomationDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideAutomationDaoFactory(databaseProvider);
  }

  public static AutomationDao provideAutomationDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAutomationDao(database));
  }
}
