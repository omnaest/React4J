package org.omnaest.react4j.data.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.data.Repository;
import org.omnaest.react4j.data.Repository.AddEntry;
import org.omnaest.react4j.data.Repository.DataEntry;
import org.omnaest.react4j.data.Repository.EntryOperationResult;
import org.omnaest.react4j.data.datagrid.internal.DataGridRepositoryProvider;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.data.provider.RepositoryProvider.Tenant;
import org.omnaest.react4j.data.provider.memory.InMemoryRepositoryProvider;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Real Spring contexts (no mocks) around the opt-in contract of {@link EnableReactUIDataGridRepository}: it registers the data grid provider; nothing
 * else - in particular not {@link EnableReactUIRepository}'s scan of the whole react4j-data package tree - registers anything from the data grid
 * module; and when the in-memory provider is present as well, a single {@link RepositoryProvider} injection yields the data grid one.
 * <p>
 * The context classes below deliberately carry <b>no</b> stereotype: this test class lives inside the very package tree that
 * {@link EnableReactUIRepository} scans, so a stereotyped nested class would be found by that scan and contaminate it.
 */
public class EnableReactUIDataGridRepositoryTest
{
    private static final String DATAGRID_PACKAGE = "org.omnaest.react4j.data.datagrid";

    @EnableReactUIDataGridRepository
    static class OptInOnly
    {
    }

    @EnableReactUIRepository
    static class RepositoryScanOnly
    {
    }

    static class NoAnnotation
    {
    }

    @EnableReactUIDataGridRepository
    @EnableReactUIInMemoryRepository
    static class OptInThenInMemoryAnnotation
    {
    }

    @EnableReactUIInMemoryRepository
    @EnableReactUIDataGridRepository
    static class InMemoryAnnotationThenOptIn
    {
    }

    @EnableReactUIDataGridRepository
    @EnableReactUIRepository
    static class OptInThenRepositoryScan
    {
    }

    @EnableReactUIRepository
    @EnableReactUIDataGridRepository
    static class RepositoryScanThenOptIn
    {
    }

    /**
     * Receives the provider exactly the way {@code ContextFactoryImpl} (react4j-core) does.
     */
    static class ProviderConsumer
    {
        @Autowired(required = false)
        RepositoryProvider repositoryProvider;
    }

    @Test
    public void optInRegistersTheDataGridProviderAndItStoresAndReadsBackAnEntry()
    {
        new ApplicationContextRunner().withUserConfiguration(OptInOnly.class)
                                      .run(context ->
                                      {
                                          assertThat(context).hasNotFailed();
                                          assertThat(context).hasSingleBean(RepositoryProvider.class);
                                          RepositoryProvider provider = context.getBean(RepositoryProvider.class);
                                          assertThat(provider).isInstanceOf(DataGridRepositoryProvider.class);

                                          Repository repository = provider.apply(Tenant.Id.of("tenant"))
                                                                          .apply(Repository.Id.of("optin", "roundtrip"));
                                          EntryOperationResult added = repository.apply(AddEntry.of(Map.of("name", "value"), null));
                                          assertThat(added.isSuccessful()).as("add failed: %s", added.getException())
                                                                          .isTrue();
                                          assertThat(repository.stream()
                                                               .map(DataEntry::asDataMap)
                                                               .collect(Collectors.toList())).containsExactly(Map.of("name", "value"));
                                      });
    }

    @Test
    public void repositoryScanWithoutTheOptInRegistersNoDataGridBean()
    {
        new ApplicationContextRunner().withInitializer(EnableReactUIDataGridRepositoryTest::makeEveryBeanLazy)
                                      .withUserConfiguration(RepositoryScanOnly.class)
                                      .run(context ->
                                      {
                                          assertThat(context).hasNotFailed();
                                          assertThat(Class.forName(DATAGRID_PACKAGE + ".internal.DataGridRepositoryProvider")).as("the data grid module is on the classpath, so the scan could find it")
                                                                                                                              .isNotNull();
                                          assertThat(beansFromTheDataGridModule(context)).isEmpty();
                                          assertThat(context.getBeansOfType(RepositoryProvider.class)
                                                            .values()).as("control: the scan ran and found the in-memory configuration")
                                                                      .hasExactlyElementsOfTypes(InMemoryRepositoryProvider.class);
                                      });
    }

    @Test
    public void contextWithNeitherAnnotationRegistersNoDataGridBean()
    {
        new ApplicationContextRunner().withInitializer(EnableReactUIDataGridRepositoryTest::makeEveryBeanLazy)
                                      .withUserConfiguration(NoAnnotation.class)
                                      .run(context ->
                                      {
                                          assertThat(context).hasNotFailed();
                                          assertThat(context).doesNotHaveBean(RepositoryProvider.class);
                                          assertThat(beansFromTheDataGridModule(context)).isEmpty();
                                      });
    }

    @Test
    public void theDataGridModuleBeanDetectorSeesTheOptInBeans()
    {
        new ApplicationContextRunner().withInitializer(EnableReactUIDataGridRepositoryTest::makeEveryBeanLazy)
                                      .withUserConfiguration(OptInOnly.class)
                                      .run(context ->
                                      {
                                          assertThat(context).hasNotFailed();
                                          assertThat(beansFromTheDataGridModule(context)).as("control: the detector used by the negative tests is not vacuous")
                                                                                         .isNotEmpty();
                                      });
    }

    @Test
    public void dataGridProviderIsInjectedWhenTheInMemoryAnnotationFollowsTheOptIn()
    {
        assertThatTheSingleInjectedProviderIsTheDataGridOne(OptInThenInMemoryAnnotation.class);
    }

    @Test
    public void dataGridProviderIsInjectedWhenTheInMemoryAnnotationPrecedesTheOptIn()
    {
        assertThatTheSingleInjectedProviderIsTheDataGridOne(InMemoryAnnotationThenOptIn.class);
    }

    @Test
    public void dataGridProviderIsInjectedWhenTheRepositoryScanFollowsTheOptIn()
    {
        assertThatTheSingleInjectedProviderIsTheDataGridOne(OptInThenRepositoryScan.class);
    }

    @Test
    public void dataGridProviderIsInjectedWhenTheRepositoryScanPrecedesTheOptIn()
    {
        assertThatTheSingleInjectedProviderIsTheDataGridOne(RepositoryScanThenOptIn.class);
    }

    private static void assertThatTheSingleInjectedProviderIsTheDataGridOne(Class<?> configuration)
    {
        new ApplicationContextRunner().withUserConfiguration(configuration)
                                      .withBean(ProviderConsumer.class)
                                      .run(context ->
                                      {
                                          assertThat(context).hasNotFailed();
                                          assertThat(context.getBean(ProviderConsumer.class).repositoryProvider).isInstanceOf(DataGridRepositoryProvider.class);
                                      });
    }

    /**
     * Makes the bean definitions lazy once all of them are registered, so a negative test inspects what was registered without starting anything.
     */
    private static void makeEveryBeanLazy(ConfigurableApplicationContext context)
    {
        context.addBeanFactoryPostProcessor(beanFactory ->
        {
            for (String name : beanFactory.getBeanDefinitionNames())
            {
                beanFactory.getBeanDefinition(name)
                           .setLazyInit(true);
            }
        });
    }

    /**
     * Names every bean whose class, factory-method declaring class or resolved type lives in a package of the data grid module (by package, so a
     * future class of that module is covered as well).
     */
    private static TreeSet<String> beansFromTheDataGridModule(org.springframework.context.ApplicationContext context)
    {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
        TreeSet<String> result = new TreeSet<>();
        for (String name : beanFactory.getBeanDefinitionNames())
        {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);
            Class<?> type = beanFactory.getType(name, false);
            boolean fromModule = isInDataGridModule(definition.getBeanClassName()) || isInDataGridModule(type != null ? type.getName() : null);
            if (definition instanceof AnnotatedBeanDefinition && ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata() != null)
            {
                fromModule |= isInDataGridModule(((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata()
                                                                                       .getDeclaringClassName());
            }
            if (fromModule)
            {
                result.add(name);
            }
        }
        return result;
    }

    private static boolean isInDataGridModule(String className)
    {
        return className != null && className.startsWith(DATAGRID_PACKAGE + ".");
    }
}
