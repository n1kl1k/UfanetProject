package com.example.un.config;


import jakarta.jms.*;
import lombok.Setter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableJms
public class ActiveMQConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Для поддержки LocalDate, LocalDateTime
        mapper.registerModule(new JavaTimeModule());
        // Чтобы не падал на неизвестных полях
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public MessageConverter jacksonMessageConverter(){
        ObjectMapper objectMapper = new ObjectMapper();
        return new MessageConverter(){
            @Override
            public Message toMessage(Object object, Session session) throws JMSException {
                try{
                String json = objectMapper.writeValueAsString(object);
                TextMessage message = session.createTextMessage(json);
                message.setStringProperty("_type", object.getClass().getName());
                return message;
                }
                catch (Exception e){
                    throw new MessageConversionException("failed to serialize", e);
                }
            }
            @Override
            public Object fromMessage(Message message) throws JMSException{
                if (message instanceof TextMessage textMessage){
                    return textMessage.getText();
                }
                throw new MessageConversionException("uncorrect message type");
            }

        };
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter
    ){
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setConcurrency("1-5");
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setSessionTransacted(true);
        return factory;
    }
}


