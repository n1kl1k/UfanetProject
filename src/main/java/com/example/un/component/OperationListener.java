package com.example.un.component;

import com.example.un.dto.AccumulativeMessage;
import com.example.un.dto.TransactionMessage;
import com.example.un.models.Reference;
import com.example.un.repository.ReferenceRepository;
import com.example.un.services.AccumulationService;
import com.example.un.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperationListener {
    private final TransactionService transactionService;
    private final AccumulationService accumulationService;
    private final ReferenceRepository referenceRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @JmsListener(
            destination = "${app.queues.transaction}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void handleTransaction(String json, Message jmsMessage) throws JMSException {
        TransactionMessage message;
        try {
            message = objectMapper.readValue(json, TransactionMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize transaction message: {}", json, e);
            jmsMessage.acknowledge();
            return;
        }


        log.info("Received transaction {}",message.getReferenceId());

        if(referenceRepository.existsByReferenceId(message.getReferenceId())){
            log.warn("duplicate message {}", message.getReferenceId());
            jmsMessage.acknowledge();
            return;
        }
        try{
            transactionService.applyTransaction(message);
            referenceRepository.save(new Reference(message.getReferenceId()));
            jmsMessage.acknowledge();
        }catch (Exception e){
            log.error("Failed to processed transaction {}", message.getReferenceId());
            throw e;
        }
    }
    @Transactional
    @JmsListener(
            destination = "${app.queues.accumulative}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void handleAccumulative(String json, Message jmsMessage)throws JMSException {
        AccumulativeMessage message;
        try {
            message = objectMapper.readValue(json, AccumulativeMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize transaction message: {}", json, e);
            jmsMessage.acknowledge();
            return;
        }
        log.info("Received accumulative {}",message.getReferenceId());

        if(referenceRepository.existsByReferenceId(message.getReferenceId())){
            log.warn("duplicate message {}", message.getReferenceId());
            jmsMessage.acknowledge();
            return;
        }
        try{
            accumulationService.applyAccumulative(message);
            referenceRepository.save(new Reference(message.getReferenceId()));
            jmsMessage.acknowledge();
        }catch (Exception e){
            log.error("Failed to processed transaction {}", message.getReferenceId());
            throw e;
        }
    }


}
