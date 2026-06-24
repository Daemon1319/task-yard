package com.allan.task_yard.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConfig {

  public static final String JOB_EXCHANGE = "job.exchange";
  public static final String JOB_QUEUE = "job.queue";
  public static final String JOB_ROUTING_KEY = "job.process";

  public static final String RETRY_EXCHANGE = "retry.exchange";
  private static final String RETRY_QUEUE_PREFIX = "retry.queue.";
  private static final String RETRY_ROUTING_KEY_PREFIX = "job.retry.";

  public static final String DLQ_EXCHANGE = "dlq.exchange";
  public static final String DLQ_QUEUE = "dlq.queue";
  public static final String DLQ_ROUTING_KEY = "job.dead";

  public static String retryRoutingKey(int attempt) {
    return RETRY_ROUTING_KEY_PREFIX + attempt;
  }

  public static String retryQueueName(int attempt) {
    return RETRY_QUEUE_PREFIX + attempt;
  }

  @Bean
  public DirectExchange jobExchange() {
    return new DirectExchange(JOB_EXCHANGE);
  }

  @Bean
  public Queue jobQueue() {
    return QueueBuilder.durable(JOB_QUEUE).build();
  }

  @Bean
  public Binding jobBinding(Queue jobQueue, DirectExchange jobExchange) {
    return BindingBuilder.bind(jobQueue).to(jobExchange).with(JOB_ROUTING_KEY);
  }

  @Bean
  public DirectExchange retryExchange() {
    return new DirectExchange(RETRY_EXCHANGE);
  }

  @Bean
  public Declarables retryQueuesAndBindings(RabbitMQProperties properties) {
    List<Declarable> declarables = new ArrayList<>();
    List<Long> delays = properties.retryDelaysMs();

    for (int i = 0; i < delays.size(); i++) {
      int attempt = i + 1;
      Queue retryQueue = QueueBuilder.durable(retryQueueName(attempt))
          .withArgument("x-message-ttl", delays.get(i))
          .withArgument("x-dead-letter-exchange", JOB_EXCHANGE)
          .withArgument("x-dead-letter-routing-key", JOB_ROUTING_KEY)
          .build();

      declarables.add(retryQueue);
      declarables.add(BindingBuilder.bind(retryQueue)
          .to(new DirectExchange(RETRY_EXCHANGE))
          .with(retryRoutingKey(attempt)));
    }

    return new Declarables(declarables);
  }

  @Bean
  public DirectExchange dlqExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  @Bean
  public Queue dlqQueue() {
    return QueueBuilder.durable(DLQ_QUEUE).build();
  }

  @Bean
  public Binding dlqBinding(Queue dlqQueue, DirectExchange dlqExchange) {
    return BindingBuilder.bind(dlqQueue).to(dlqExchange).with(DLQ_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory,
      MessageConverter jsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(jsonMessageConverter);
    return factory;
  }
}