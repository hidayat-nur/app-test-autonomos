package com.appautomation.presentation.viewmodel;

import android.content.Context;
import com.appautomation.data.repository.AppRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppRepository> repositoryProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppRepository> repositoryProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), repositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppRepository> repositoryProvider) {
    return new SettingsViewModel_Factory(contextProvider, repositoryProvider);
  }

  public static SettingsViewModel newInstance(Context context, AppRepository repository) {
    return new SettingsViewModel(context, repository);
  }
}
