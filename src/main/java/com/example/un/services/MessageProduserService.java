package com.example.un.services;

import com.example.un.dto.AccumulativeMessage;
import com.example.un.dto.TransactionMessage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProduserService {
    private final JmsTemplate jmsTemplate;

    @Value("${app.queues.transaction}")
    private String transactionQueue;
    @Value("${app.queues.accumulative}")
    private String accumulativeQueue;

    public void sendTransaction(TransactionMessage message){
        jmsTemplate.convertAndSend(transactionQueue,message);
    }
    public void sendAccumulative(AccumulativeMessage message){
        jmsTemplate.convertAndSend(accumulativeQueue,message);
    }
}
